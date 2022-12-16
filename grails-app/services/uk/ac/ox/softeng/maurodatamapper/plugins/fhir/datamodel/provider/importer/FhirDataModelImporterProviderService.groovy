/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.ImportDataHandling
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.MetadataHandling
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.exporter.FhirDataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

@Slf4j
class FhirDataModelImporterProviderService extends DataModelImporterProviderService<FhirDataModelImporterProviderServiceParameters>
    implements MetadataHandling, ImportDataHandling<DataModel, FhirDataModelImporterProviderServiceParameters> {

    private static List<String> NON_METADATA_KEYS = ['id', 'definition', 'description', 'min', 'max', 'alias', 'publisher', 'snapshot',
                                                     'differential']

    AuthorityService authorityService

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
    String getNamespace() {
        'uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel'
    }

    @Override
    Boolean handlesContentType(String contentType) {
        contentType.equalsIgnoreCase(FhirDataModelExporterProviderService.CONTENT_TYPE)
    }

    @Override
    Boolean canFederate() {
        false
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
    DataModel updateImportedModelFromParameters(DataModel importedModel, FhirDataModelImporterProviderServiceParameters params, boolean list) {
        updateFhirImportedModelFromParameters(importedModel, params, list)
    }

    @Override
    DataModel checkImport(User currentUser, DataModel importedModel, FhirDataModelImporterProviderServiceParameters params) {
        DataModel checked = checkFhirImport(currentUser, importedModel, params)

        checked.dataClasses?.each {dc ->
            classifierService.checkClassifiers(currentUser, dc)
            dc.dataElements?.each {de ->
                classifierService.checkClassifiers(currentUser, de)
            }
        }
        checked.dataTypes?.each {dt ->
            classifierService.checkClassifiers(currentUser, dt)
        }
        checked
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
        DataModel dataModel = new DataModel(label: data.id, description: data.description, organisation: data.publisher,
                                            authority: findOrCreateAuthority(data, fhirServerClient, currentUser))
        if (data.alias) {
            dataModel.aliases = data.alias as List<String>
        }
        processMetadata(data, dataModel, namespace, NON_METADATA_KEYS)

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

        // find all DCs
        List<String> dataClassKeys = dataItemMap
            .findAll {key, value ->
                value.last() == 'id'
            }.collect {key, value ->
            value.findAll {it != 'id'}.join('.')
        }.sort()

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
            processDataElement(dataset, parentDataClass, dataModel)
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
        Path parentDataClassPath = buildDataClassPath(dataModel.path, keySections)
        dataModel.dataClasses.find {dataClass ->
            if (parentDataClassPath == dataClass.path) {
                dataClass
            }
        }
    }

    private static Path buildDataClassPath(Path dataModelPath, List<String> dataClassLabelsInPath){
        Path path = Path.from(dataModelPath.last())
        dataClassLabelsInPath.each{
            path.addToPathNodes('dc', it, false)
        }
        path
    }

    private void processDataClass(Map dataset, DataClass parentDataClass, DataModel dataModel) {
        DataClass dataClass = new DataClass(label: dataset.id.tokenize('.').last(), path: dataset.id, description: dataset.definition)
        if (dataset.alias) {
            dataClass.aliases = dataset.alias as List<String>
        }
        dataClass.minMultiplicity = parseInt(dataset.min)
        dataClass.maxMultiplicity = parseInt((dataset.max == '*' ? -1 : dataset.max))
        log.debug('Created dataClass {}', dataClass.label)
        processMetadata(dataset, dataClass, namespace, NON_METADATA_KEYS)
        if (parentDataClass) {
            parentDataClass.addToDataClasses(dataClass)
        }
        dataModel.addToDataClasses(dataClass)
        dataClass
    }

    private void processDataElement(Map dataset, DataClass parentDataClass, DataModel dataModel) {
        if (!parentDataClass) {
            // There seem to some odd entries which have no parent, looking at the JSON they seem to be coded under a different ID as well,
            // so seems safe to exclude
            log.warn('Could not add {} as no parent dataclass exists', dataset.id)
            return
        }
        DataElement dataElement = new DataElement(label: dataset.id.tokenize('.').last(), path: dataset.id, description: dataset.definition)
        if (dataset.alias) {
            dataElement.aliases = dataset.alias as List<String>
        }
        String dTLabel = dataset.type ? dataset.type.code.get(0) : dataset.contentReference
        PrimitiveType pt = dataModel.dataTypes.find {it.label == dTLabel}
        if (!pt) {
            pt = new PrimitiveType(label: dTLabel)
            dataModel.addToPrimitiveTypes(pt)
        }
        dataElement.dataType = pt
        dataElement.minMultiplicity = parseInt(dataset.min)
        dataElement.maxMultiplicity = parseInt((dataset.max == '*' ? -1 : dataset.max))
        log.debug('Created dataElement {}', dataElement.label)
        processMetadata(dataset, dataElement, namespace, NON_METADATA_KEYS)
        parentDataClass.addToDataElements(dataElement)
        dataElement
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
