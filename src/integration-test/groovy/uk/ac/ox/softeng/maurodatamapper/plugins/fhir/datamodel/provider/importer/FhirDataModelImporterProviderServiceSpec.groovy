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

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.FhirDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Integration
class FhirDataModelImporterProviderServiceSpec extends BaseIntegrationSpec {
    FhirDataModelImporterProviderService fhirDataModelImporterProviderService
    @Shared
    Path resourcesPath

    @OnceBefore
    void setupServerClient() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources').toAbsolutePath()
        fhirDataModelImporterProviderService.fhirServerClient = Stub(FhirServerClient) {
            it.getStructureDefinitionEntry(_) >> {String entryId ->
                String content = loadTestFileAsString("${entryId}.json")
                new JsonSlurper().parseText(content)
            }
        }
    }

    @Override
    void setupDomainData() {
    }

    def 'CC01: Test importing CareConnect-ProcedureRequest-1 datamodel'() {
        given:
        String entryId = 'CareConnect-ProcedureRequest-1'
        def parameters = new FhirDataModelImporterProviderServiceParameters(
            modelName: entryId
        )
        when:
        DataModel dataModel = fhirDataModelImporterProviderService.importModel(admin, parameters)
        then:
        dataModel
        dataModel.label == entryId

        dataModel.dataClasses.size() == 13
        dataModel.metadata.size() == 19

        when:
        DataClass dataClass = dataModel.childDataClasses.find { it.label == 'ProcedureRequest' }

        then:
        dataClass
        dataClass.minMultiplicity == 0
        dataClass.maxMultiplicity == -1
        dataClass.metadata.size() == 36
        dataClass.dataElements.size() == 100

        when:
        DataElement dataElement = dataClass.dataElements.find { it.label == 'ProcedureRequest.id' }

        then:
        dataElement
        dataElement.description == 'The logical id of the resource, as used in the URL for the resource. Once assigned, this value never changes.'
        dataElement.minMultiplicity == 0
        dataElement.maxMultiplicity == 1
        dataElement.metadata.size() == 16

        dataClass.dataClasses.size() == 12
    }

    def 'CC02: Test importing CareConnect-OxygenSaturation-Observation-1 datamodel'() {
        given:
        String entryId = 'CareConnect-OxygenSaturation-Observation-1'
        def parameters = new FhirDataModelImporterProviderServiceParameters(
            modelName: entryId
        )
        when:
        DataModel dataModel = fhirDataModelImporterProviderService.importModel(admin, parameters)
        then:
        dataModel
        dataModel.label == entryId

        when:
        DataClass dataClass = dataModel.dataClasses.find { it.label == 'Observation.valueQuantity' }
        then:
        (dataClass.metadata.find { it.key == 'sliceName' }).value == 'valueQuantity'
    }

    def 'Test importing multiple datamodel'() {
        given:
        def parameters = new FhirDataModelImporterProviderServiceParameters()
        when:
        List<DataModel> imported = fhirDataModelImporterProviderService.importModels(admin, parameters)
        then:
        // STU3 has 111 models
        imported.size() == 111
    }

    String loadTestFileAsString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }
}