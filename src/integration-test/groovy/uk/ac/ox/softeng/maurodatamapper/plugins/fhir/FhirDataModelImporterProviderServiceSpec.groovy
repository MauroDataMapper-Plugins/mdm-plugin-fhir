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

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.FhirDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirCodeSystemTerminologyService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FihrTerminologyImporterService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.TestUser

import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Integration
class FhirDataModelImporterProviderServiceSpec extends BaseFunctionalSpec {

    @Shared
    Path resourcesPath

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources').toAbsolutePath()
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    String loadTestFileAsString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }


    User getAdmin() {
        new TestUser(emailAddress: StandardEmailAddress.ADMIN,
                firstName: 'Admin',
                lastName: 'User',
                organisation: 'Oxford BRC Informatics',
                jobTitle: 'God',
                id: UUID.randomUUID())
    }

    def "verify Code system dataModel"() {
        def fileAsString = loadTestFileAsString('fhir-server-code-systems-payload.json')
      //  FHIRServerClient serverClient = Mock()
        FHIRServerClient serverClient = Stub(FHIRServerClient) {
            it.getCodeSystemTerminologies('json') >> fileAsString
        }
        FihrTerminologyImporterService fhir = new FihrTerminologyImporterService()
        def parameters = new FhirTerminologyImporterProviderServiceParameters()

        given:
        fhir.serverClient = serverClient

        when:
        def terminology = fhir.importModel(admin, parameters)

        then:
        1 * serverClient._
    }

    def "verify Structure Definition dataModel"() {
        def fileAsString = loadTestFileAsString('fhir-server-structure-definition-payload.json')
        DataModelService dataModelService = Mock()
        FHIRServerClient serverClient = Stub(FHIRServerClient) {
            it.getStructureDefinition('json') >> fileAsString
        }
        FhirDataModelImporterProviderService fhir = new FhirDataModelImporterProviderService()
        def parameters = new FhirDataModelImporterProviderServiceParameters()

        given:
        fhir.dataModelService = dataModelService
        fhir.serverClient = serverClient

        when:
        def dataModel = fhir.importModel(admin, parameters)

        then:
        1 * dataModelService._
        //assert(dataModel.label=='')
    }


    @Override
    String getResourcePath() {
        ''
    }
}
