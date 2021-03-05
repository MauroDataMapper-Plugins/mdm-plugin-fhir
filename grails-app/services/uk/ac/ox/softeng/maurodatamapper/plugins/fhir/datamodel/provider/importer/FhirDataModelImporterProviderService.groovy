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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
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

        //dataModel metadata
        data.each {key, value ->
            // If key not in list then add to MD
            if (!(key in ['id', 'description', 'differential', 'snapshot'])) {
                dataModel.addToMetadata(
                    namespace: namespace,
                    key: key,
                    value: value.toString()
                )
            }

        }

        def datasets = data.snapshot.element
        // TODO append datasets with data.differential.element

        //dataClass and dataElement list
        def idList = []
        datasets.each {dataMap ->
            String dataMapId = dataMap.id
            idList.add(dataMapId)
        }
        idList.sort()
        //not sure if this is necessary; it already appears well-organised, don't know if there is pre-existing meaning in their ordering
        int x = 0
        idList.each {
            if (x == idList.size()) {
                dataElement
            } else if ((idList[x + 1]).substring(0, len(idList[x])) == idList[x]) {
                dataClass
                currentDataClass = dataClass
            } else {
                //dataElement
                //add to currentDataClass
            }
            x += 1
        }


        datasets.each {dataMap ->
            DataClass dataClass = new DataClass()
            dataClass.label = dataMap.id
            dataClass.description = dataMap.definition
            if (dataMap.max == '*') {
                dataClass.maxMultiplicity = '-1'.toInteger()
            } else {
                dataClass.maxMultiplicity = dataMap.max.toInteger()
            }
            dataClass.minMultiplicity = dataMap.min.toInteger()

            dataMap.each {
                if (it.key != 'id'
                    && it.key != 'definition'
                    && it.key != 'constraint') {
                    dataClass.addToMetadata(new Metadata(
                        namespace: namespace,
                        key: it.key,
                        value: it.value.toString()
                    ))
                }
            }

            def constraintList = dataMap.constraint

            if (constraintList) {
                constraintList.each {constraintMap ->
                    constraintMap.each {
                        dataClass.addToMetadata(new Metadata(
                            namespace: namespace,
                            key: it.key,
                            value: it.value.toString()
                        ))
                    }
                }
            }
            dataModel.addToDataClasses(dataClass)
        }
        dataModelService.checkImportedDataModelAssociations(currentUser, dataModel)
        dataModel
    }

}
