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
        if (definition.total > 1) {
            definition = fhirServerClient.getStructureDefinition(definition.total as int)
        }

        // Collect all the entries as datamodels
        definition.entry.collect {Map entry ->
            importDataModel(user, entry.resource.id)
        }
    }

    private DataModel importDataModel(User currentUser, String dataModelName) {
        log.debug('Importing DataModel {} from FHIR', dataModelName)

        // Load the map for that datamodel name
        Map<String, Object> data = fhirServerClient.getStructureDefinitionEntry(dataModelName)

        //dataModel initialisation
        DataModel dataModel = new DataModel(name: dataModelName, description: data.description)
        processMetadata(data, dataModel)

        def datasets = data.snapshot.element
        // TODO resolve data.differential.element

        List<String> idList = []
        datasets.each { dataMap ->
            String dataMapId = dataMap.id
            idList.add(dataMapId)
        }
        //idList.sort()

        Boolean hasChildren = false
        datasets.each { dataset ->
            childCheck(idList, dataset)
            if (hasChild = false) {
                processDataElement(dataset, dataModel)
            } else if (hasChild = true) {
                processDataClass(dataset, dataModel, hasChildren, idList)
            }
        }
        dataModelService.checkImportedDataModelAssociations(currentUser, dataModel)
        dataModel
    }

    private static Boolean childCheck(List idList, dataset) {
        Boolean hasChild = false
        int x = (idList.indexOf(dataset.id))
        if (x == idList.size() - 1) {

        } else if ((idList[x + 1].length() > idList[x].length()) && ((idList[x] + '.') == ((idList[x + 1]).substring(0, (idList[x].length() + 1))))) {
            hasChild = true
        } else {

        }
        hasChild
    }

    private void processDataClass(Map dataset, DataModel dataModel, Boolean hasChildren, List idList) {
        DataClass dataClass = new DataClass(name: dataset.id, description: dataset.definition)
        dataClass.minMultiplicity = dataset.min.toInteger()
        dataClass.maxMultiplicity = (dataset.max == '*' ? -1 : dataset.max).toInteger()
        log.debug('Created dataclass {}', dataClass.label)
        childCheck(idList, dataset)
        if (hasChild = true) {

            DataClass childDataClass = processDataClass(dataset, dataClass)
            // dataset of child
            dataClass.addToDataClasses(childDataClass)
        } else if (hasChild = false) {
            processDataElement(dataset, dataClass)
        } else {
            throw new ApiInternalException('FHIR03', 'Unclear if DataClass ${dataClass} has children')
        }
        dataModel.addToDataClasses(dataClass)
        dataClass
    }

    private void processDataElement(Map dataset, DataClass dataClass) {
        DataElement dataElement = new DataElement(name: dataset.id, description: dataset.definition)
        dataElement.minMultiplicity = dataset.min.toInteger()
        dataElement.maxMultiplicity = (dataset.max == '*' ? -1 : dataset.max).toInteger()
        processMetadata(dataset, dataElement)
        dataClass.addToDataElements(dataElement)
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

}
