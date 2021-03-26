/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
class FhirDataModelImporterProviderService extends DataModelImporterProviderService<FhirDataModelImporterProviderServiceParameters> {

    @Autowired
    ApplicationContext applicationContext

    @Override
    String getDisplayName() {
        'FHIR Server DataModel Importer'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    Boolean allowsExtraMetadataKeys() {
        true
    }

    @Override
    Boolean canImportMultipleDomains() {
        true
    }

    @Override
    DataModel importModel(User user, FhirDataModelImporterProviderServiceParameters params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        if (!params.modelName) throw new ApiBadRequestException('FHIR02', 'Cannot import a single datamodel without the datamodel name')
        log.debug('Import DataModel {}', params.modelName)
        FhirServerClient fhirServerClient = new FhirServerClient(params.fhirHost, params.fhirVersion, applicationContext)
        importDataModel(fhirServerClient, user, params.fhirVersion, params.modelName)
    }

    @Override
    List<DataModel> importModels(User user, FhirDataModelImporterProviderServiceParameters params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')

        if (params.modelName) {
            log.debug('Model name supplied, only importing 1 model')
            return [importModel(user, params)]
        }

        log.debug('Import DataModels version {}', params.fhirVersion ?: 'Current')
        FhirServerClient fhirServerClient = new FhirServerClient(params.fhirHost, params.fhirVersion, applicationContext)
        // Just get the first entry as this will tell us how many there are
        Map<String, Object> countResponse = fhirServerClient.getStructureDefinitionCount()

        // Now get the full list
        Map<String, Object> definitions = fhirServerClient.getStructureDefinitions(countResponse.total as int)

        // Collect all the entries as datamodels
        definitions.entry.collect {Map entry ->
            importDataModel(fhirServerClient, user, params.fhirVersion, entry.resource.id)
        }
    }

    private DataModel importDataModel(FhirServerClient fhirServerClient, User currentUser, String version, String dataModelName) {
        log.debug('Importing DataModel {} from FHIR version {}', dataModelName, version ?: 'Current')

        // Load the map for that datamodel name
        Map<String, Object> data = fhirServerClient.getStructureDefinitionEntry(dataModelName)

        //log.trace('JSON\n{}', new JsonBuilder(data).toPrettyString())

        //dataModel initialisation
        DataModel dataModel = new DataModel(label: dataModelName, description: data.description)
        processMetadata(data, dataModel)

        List<Map> datasets = data.snapshot.element

        // resolving odd id names eg OxygenSaturation valueQuantity
        datasets = datasets.each {dataset ->
            if (dataset.sliceName && (dataset.path.tokenize('.').last() == dataset.sliceName)) {
                String accursedSliceName = dataset.sliceName
                datasets.each {pathDataset ->
                    if (pathDataset.id.contains(accursedSliceName)) {
                        pathDataset.oldId = pathDataset.id
                        pathDataset.id = pathDataset.path
                        log.debug('changed id from {} to {}', pathDataset.oldId, pathDataset.id)
                    }
                }
            }
        }

        List<String> datasetIds = datasets.collect {it.id} as List<String>

        Map<String, List<String>> dataItemMap = datasetIds.collectEntries {id ->
            [id, id.tokenize('.')]
        }

        // find all dcs
        List<String> dataClassKeys = dataItemMap
            .findAll {key, value ->
                value.last() == 'id'
            }.collect {key, value ->
            value.findAll {it != 'id'}.join('.')
        }

        log.debug('{} DataClasses found', dataClassKeys.size())

        //Iterate through DCs and map and add to parent DC
        dataClassKeys.each {dataClassKey ->
            Map dataset = datasets.find {dataset -> dataset.id == dataClassKey}
            DataClass parentDataClass = findParentDataClass(dataClassKey, dataModel)
            processDataClass(dataset, parentDataClass, dataModel)
        }

        //Iterate Map for elements
        dataItemMap.findAll {key, value ->
            !(key in dataClassKeys)
        }.each {key, value ->
            Map dataset = datasets.find {dataset -> dataset.id == key}
            DataClass parentDataClass = findParentDataClass(key, dataModel)
            processDataElement(dataset, parentDataClass)
            //DataClass referenceDc = dataClasses.find{it.label = value.last()}
        }

        //dataItemMap.findAll { key, value ->
        //    referenceDCs = dataClassKeys.find { it.label = value.last() }
        //}

        dataModelService.checkImportedDataModelAssociations(currentUser, dataModel)
        dataModel
    }

    private static DataClass findParentDataClass(String dataClassKey, DataModel dataModel) {
        List<String> keySections = dataClassKey.tokenize('.')
        keySections.removeLast()
        if (!keySections) return null
        String parentDataClass = keySections.join('.')
        dataModel.dataClasses.find {dataClass ->
            if (parentDataClass == dataClass.path) {
                dataClass
            }
        }
    }

    private void processDataClass(Map dataset, DataClass parentDataClass, DataModel dataModel) {
        DataClass dataClass = new DataClass(label: dataset.id.tokenize('.').last(), path: dataset.id, description: dataset.definition)
        dataClass.minMultiplicity = parseInt(dataset.min)
        dataClass.maxMultiplicity = parseInt((dataset.max == '*' ? -1 : dataset.max))
        log.debug('Created dataClass {}', dataClass.label)
        processMetadata(dataset, dataClass)
        if (parentDataClass) {
            parentDataClass.addToDataClasses(dataClass)
        }
        dataModel.addToDataClasses(dataClass)
        dataClass
    }

    private void processDataElement(Map dataset, DataClass parentDataClass) {
        if (!parentDataClass) {
            // There seem to some odd entries which have no parent, looking at the JSON they seem to be coded under a different ID as well,
            // so seems safe to exclude
            log.warn('Could not add {} as no parent dataclass exists', dataset.id)
            return
        }
        DataElement dataElement = new DataElement(label: dataset.id.tokenize('.').last(), path: dataset.id, description: dataset.definition)
        dataElement.minMultiplicity = parseInt(dataset.min)
        dataElement.maxMultiplicity = parseInt((dataset.max == '*' ? -1 : dataset.max))
        log.debug('Created dataElement {}', dataElement.label)
        processMetadata(dataset, dataElement)
        parentDataClass.addToDataElements(dataElement)
        dataElement
    }

    private void processMetadata(dataset, dataItem) {
        List<String> nonMetadata = ['id', 'definition', 'description', 'min', 'max', 'snapshot', 'differential']
        dataset.each {key, value ->
            if (!(key in nonMetadata)) {
                if (!(value instanceof String || value instanceof Integer || value instanceof Boolean)) {
                    processNestedMetadata(key, value, dataItem)
                } else {
                    dataItem.addToMetadata(
                        namespace: namespace,
                        key: key,
                        value: value.toString()
                    )
                }
            }
        }
    }

    private void processNestedMetadata(key, dataCollection, dataItem) {
        try {
            if (dataCollection instanceof List) {
                processListedMetadata(key, dataCollection, dataItem)
            }
            if (dataCollection instanceof Map) {
                processMappedMetadata(key, dataCollection, dataItem)
            }
        } catch (Exception ex) {
            throw new ApiInternalException('FHIR04', 'bad nesting, nests are either map or list', ex)
        }
    }

    private void processListedMetadata(key, List list, dataItem) {
        if (list.size() > 1) {
            list.eachWithIndex {item, index ->
                if (item instanceof String || item instanceof Integer || item instanceof Boolean) {
                    dataItem.addToMetadata(
                        namespace: namespace,
                        key: "$key[${index}]",
                        value: item.toString()
                    )
                } else if (item instanceof Map) {
                    processMappedMetadata("$key[${index}]", item, dataItem)
                }
            }
        } else {
            list.each {item ->
                if (item instanceof String || item instanceof Integer || item instanceof Boolean) {
                    dataItem.addToMetadata(
                        namespace: namespace,
                        key: key,
                        value: item.toString()
                    )
                } else if (item instanceof Map) {
                    processMappedMetadata(key, item, dataItem)
                }
            }
        }
    }

    private void processMappedMetadata(String key, Map map, dataItem) {
        map.each {mapKey, mapVal ->
            if (mapVal instanceof String || mapVal instanceof Integer || mapVal instanceof Boolean) {
                dataItem.addToMetadata(
                    namespace: namespace,
                    key: "${key}.${mapKey}",
                    value: mapVal.toString()
                )
            }
            if (mapVal instanceof List) {
                processListedMetadata("${key}.${mapKey}", mapVal, dataItem)
            }
        }
    }

    private static Integer parseInt(def value) {
        if (value instanceof Number) return value.toInteger()
        if (value instanceof String) {
            try {
                return value.toInteger()
            } catch (NumberFormatException ignored) {
            }
        }
        null
    }
}
