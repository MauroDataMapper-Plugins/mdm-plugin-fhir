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
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class FhirDataModelImporterProviderService extends DataModelImporterProviderService<FhirDataModelImporterProviderServiceParameters> {

    @Autowired
    FhirServerClient fhirServerClient

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
        log.debug('Import DataModel')
        importDataModel(user, params.modelName)
    }

    @Override
    List<DataModel> importModels(User user, FhirDataModelImporterProviderServiceParameters params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        log.debug('Import DataModels')
        // Just get the first entry as this will tell us how many there are
        Map definition = fhirServerClient.getStructureDefinition(1)

        // Now get the full list
        try {
            if (definition.total > 1) {
                definition = fhirServerClient.getStructureDefinition(definition.total as int)
            }
        } catch (Exception ex) {
            throw new ApiInternalException('FHIR03', 'Could not import Fhir models', ex)
        }

        // Collect all the entries as datamodels
        definition.entry.collect { Map entry ->
            importDataModel(user, entry.resource.id)
        }
    }

    private DataModel importDataModel(User currentUser, String dataModelName) {
        // TODO not confident about namespace - appears to be "uk.ac.ox.soft.....plugins.fhir.datamodel.provider.importer"
        log.debug('Importing DataModel {} from FHIR', dataModelName)

        // Load the map for that datamodel name
        Map<String, Object> data = fhirServerClient.getStructureDefinitionEntry(dataModelName)

        //dataModel initialisation
        DataModel dataModel = new DataModel(label: dataModelName, description: data.description)
        processMetadata(data, dataModel)

        List<Map> datasets = data.snapshot.element
        // TODO resolve data.differential.element

        List<String> datasetIds = datasets.collect { it.id } as List<String>

        Map<String, List<String>> dataItemMap = datasetIds.collectEntries { id ->
            [id, id.tokenize('.').toList()]
        }

        // find all dcs
        List<String> dataClassKeys = dataItemMap
            .findAll { key, value ->
                value.last() == 'id'
            }.collect { key, value ->
            value.findAll { it != 'id' }.join('.')
        }

        log.debug('stop')

        //Iterate through DCs and map and add to parent DC
        dataClassKeys.each { dataClassKey ->
            Map dataset = datasets.find { dataset -> dataset.id == dataClassKey }
            DataClass parentDataClass = findParentDataClass(dataClassKey, dataModel)
            processDataClass(dataset, parentDataClass, dataModel)
        }

        //Iterate Map for elements
        dataItemMap.findAll { key, value ->
            !(key in dataClassKeys)
        }.each { key, value ->
            Map dataset = datasets.find { dataset -> dataset.id == key }
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
        dataModel.dataClasses.find { dataClass ->
            parentDataClass == dataClass.label
            dataClass
        }
    }

    private void processDataClass(Map dataset, DataClass parentDataClass, DataModel dataModel) {
        DataClass dataClass = new DataClass(label: dataset.id, description: dataset.definition)
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
        DataElement dataElement = new DataElement(label: dataset.id, description: dataset.definition)
        dataElement.minMultiplicity = parseInt(dataset.min)
        dataElement.maxMultiplicity = parseInt((dataset.max == '*' ? -1 : dataset.max))
        log.debug('Created dataElement {}', dataElement.label)
        processMetadata(dataset, dataElement)
        parentDataClass.addToDataElements(dataElement)
        dataElement
    }

    private void processMetadata(dataset, dataItem) {
        List<String> nestedData = ['alias', 'base', 'constraint', 'mapping']
        dataset.each { key, value ->
            if (!(key in (['id', 'definition', 'differential', 'snapshot'] + nestedData))) {
                dataItem.addToMetadata(new Metadata(
                    namespace: namespace,
                    key: key,
                    value: value.toString()
                ))
            }
            if (key in nestedData) {
                def keyList = dataset.key
                keyList.each { dataMap ->
                    dataMap.each {
                        dataItem.addToMetadata(new Metadata(
                            namespace: namespace,
                            key: key,
                            value: value.toString()
                        ))
                    }
                }
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
