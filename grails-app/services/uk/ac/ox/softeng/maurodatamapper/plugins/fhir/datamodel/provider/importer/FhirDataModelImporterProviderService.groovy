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

import groovy.json.JsonSlurper
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestClientException

@Slf4j
class FhirDataModelImporterProviderService extends DataModelImporterProviderService<FhirDataModelImporterProviderServiceParameters> {

    @Autowired
    DataModelService dataModelService

    @Autowired
    FHIRServerClient serverClient

    @Override
    String getDisplayName() {
        'FHIR Server Importer'
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
    DataModel importModel(User user, FhirDataModelImporterProviderServiceParameters t) {
        log.debug("importDataModel")
        importModels(user, t)?.first()
    }

    @Override
    List<DataModel> importModels(User currentUser, FhirDataModelImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        log.debug("importDataModels")
        try {
           def response = new JsonSlurper().parseText(serverClient.getStructureDefinition('json'))
            log.debug('Parsing in file content using JsonSlurper')
            importDataModels(currentUser, response)
        } catch (RestClientException e) {
            throw new ApiInternalException('FHIR02', 'Error making webservice call to FHIR server ' + e)
        }
    }

    private List<DataModel> importDataModels(User currentUser, def data) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR0101', 'User must be logged in to import model')

        String namespace = "org.fhir.server"
        List<DataModel> imported = []

        DataModel dataModel = new DataModel()
        dataModel.label = data.name
        dataModel.description = data.description

        data.each { dataMap ->
            dataMap.each {
                if (it.key != 'id' && it.key != 'description') {
                    dataModel.addToMetadata(new Metadata(
                            namespace: namespace,
                            key: it.key,
                            value: it.value.toString()
                    ))
                }
            }
        }

        def datasets = data.snapshot.element
        //def datasets = data.differential.element

        datasets.each { dataMap ->

            DataClass dataClass = new DataClass()
            dataClass.label = dataMap.id
            dataClass.description = dataMap.definition
            dataClass.maxMultiplicity = dataMap.max
            dataClass.minMultiplicity = dataMap.min

            dataMap.each {
                if (it.key != 'id' && it.key != 'description') {
                    dataModel.addToMetadata(new Metadata(
                            namespace: namespace,
                            key: it.key,
                            value: it.value.toString()
                    ))
                }
            }

            def constraintList = dataMap.constraint

            if (constraintList) {
                constraintList.each {
                    dataClass.addToMetadata(new Metadata(
                            namespace: namespace,
                            key: it.key,
                            value: it.value.toString()
                    ))
                }
            }
            dataModel.addToDataClasses(dataClass)

            imported += dataModel
            dataModelService.checkImportedDataModelAssociations(currentUser, dataModel)
        }
        imported
    }

    @Override
    Boolean canImportMultipleDomains() {
        return null
    }
}
