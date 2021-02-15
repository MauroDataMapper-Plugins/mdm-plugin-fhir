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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestClientException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.DataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User

import java.nio.charset.Charset

@Slf4j
class FHIRDataModelImporterProviderService extends DataModelImporterProviderService<FHIRDataModelImporterProviderServiceParameters> {

    @Autowired
    DataModelService dataModelService

    @Autowired
    FHIRServerClient serverClient

    @Override
    String getDisplayName() {
        'FHIR Importer'
    }

    @Override
    String getVersion() {
        '2.1.0-SNAPSHOT'
    }

    @Override
    Boolean allowsExtraMetadataKeys() {
        true
    }

    @Override
    DataModel importModel(User user, FHIRDataModelImporterProviderServiceParameters t) {
        log.debug("importDataModel")
        importModels(user, t)?.first()
    }

    @Override
    List<DataModel> importModels(User currentUser, FHIRDataModelImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        //is there a consensus on error coding
        //FileParameter importFile = params.importFile
        //if (!importFile.fileContents.size()) throw new ApiBadRequestException('FHIR0001', 'Cannot import empty file')
        try {
            if (params.importType == 'structureDefinition') {
                def response = new JsonSlurper().parseText(serverClient.getStructureDefinition('json'))
                log.debug('Parsing in file content using JsonSlurper')
                importDataModels(currentUser, response)
            }
        } catch (RestClientException e) {
            throw new ApiInternalException('FHIR02', 'Error making webservice call to FHIR server ' + e)
        }
    }

    private List<DataModel> importDataModels(User currentUser, def data) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR0101', 'User must be logged in to import model')
        //error coding?

        String namespace = "org.fhir.server"
        List<DataModel> imported = []

        DataModel dataModel = new DataModel()
        dataModel.label = data.name
        dataModel.description = data.description

        def datasets = data.snapshot.element

        try {
            datasets.each { dataset ->

                DataElement dataElement = new DataElement()
                DataType itemDataType = new PrimitiveType()
                //String uniqueName = dataset.id
                dataElement.dataType = itemDataType
                dataElement.description = dataset.definition
                dataElement.maxMultiplicity = dataset.max

                dataset.each {dataMap ->
                    dataMap
                    dataElement.addToMetadata(new Metadata(
                            namespace: namespace,
                            key: dataMap.key,
                            value: dataMap.value
                    ))
                }
                imported += dataModel

            }
        } catch (Exception ex) {
            throw new ApiInternalException('FHIR02', "${ex.message}")
        }
        imported
    }

    @Override
    Boolean canImportMultipleDomains() {
        return null
    }
}
