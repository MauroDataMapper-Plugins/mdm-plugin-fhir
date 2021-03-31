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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.exporter.FhirCodeSetJsonExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.FhirCodeSetImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter.FhirCodeSetImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirTerminologyImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import com.stehno.ersatz.ErsatzServer
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import grails.validation.ValidationException
import groovy.util.logging.Slf4j
import org.junit.Assert
import spock.lang.Shared
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Integration
@Rollback
@Slf4j
class FhirCodeSetJsonExporterProviderServiceSpec extends BaseIntegrationSpec implements JsonComparer {

    FhirCodeSetImporterProviderService fhirCodeSetImporterProviderService
    FhirCodeSetJsonExporterProviderService fhirCodeSetJsonExporterProviderService
    FhirTerminologyImporterProviderService fhirTerminologyImporterProviderService
    CodeSetService codeSetService
    TerminologyService terminologyService

    @Shared
    Path resourcesPath

    @Shared
    Path terminologyResourcesPath    

    @OnceBefore
    void setupServerClient() {
        resourcesPath =
            Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'value_sets').toAbsolutePath()

        terminologyResourcesPath =
            Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'code_systems').toAbsolutePath()            
        ersatz = new ErsatzServer()
    }

    @Shared
    ErsatzServer ersatz

    void cleanup() {
        ersatz.clearExpectations()
    }

    void cleanupSpec() {
        ersatz.stop()
    }

    @Override
    void setupDomainData() {
        folder = new Folder(label: 'catalogue', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        checkAndSave(folder)
    }    

    @Unroll
    def 'CC01: verify exported CodeSet JSON content - "#testName"'() {
        //import FHIR Json file
        given:
        setupDomainData()
        String entryId = testName
        String exportedEntryId = "${entryId}_exported"
        String version = 'STU3'
        importTerminology(entryId)
        ersatz.expectations {
            GET("/$version/ValueSet/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
            GET("/$version/ValueSet/$exportedEntryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${exportedEntryId}.json"))
                }
            }
        }
        def parameters = new FhirCodeSetImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when: 'the CodeSet is imported from Json'
        CodeSet imported = fhirCodeSetImporterProviderService.importModel(admin, parameters)
        
        then: 'it is imported with the correct label'
        imported
        imported.label == entryId

        when: 'the imported CodeSet is exported'
        ByteArrayOutputStream exportedJsonBytes = (fhirCodeSetJsonExporterProviderService.exportCodeSet(admin, imported))
        String exportedJson = new String(exportedJsonBytes.toByteArray())

        then: 'the exported Json is correct'
        exportedJson
        validateExportedModel(entryId, exportedJson)


        def reParameters = new FhirCodeSetImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: exportedEntryId
        )

        when: 'the codeset is reimported from the export'
        CodeSet reImported = fhirCodeSetImporterProviderService.importModel(admin, reParameters)

        then:
        reImported

        when: 'differences between the import and reimport are determined'
        ObjectDiff od = codeSetService.getDiffForModels(imported, reImported)

        then: 'there are no differences'
        od.getNumberOfDiffs() == 0

        where:
        testName << [
            'CareConnect-ConditionCategory-1',
            'CareConnect-EthnicCategory-1'
        ]
    }

    String loadJsonString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }

    String loadTerminologyJsonString(String filename) {
        Path testFilePath = terminologyResourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }    

    void validateExportedModel(String entryId, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${entryId}_exported.json")
        if (!Files.exists(expectedPath)) {
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }

        String expectedJson = replaceContentWithMatchers(Files.readString(expectedPath))
        verifyJson(expectedJson, exportedModel)
    }

    private void importTerminology(String terminologyId) {
        ersatz.expectations {
            GET("/STU3/CodeSystem/$terminologyId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadTerminologyJsonString("${terminologyId}.json"))
                }
            }
        }
        def parameters = new FhirTerminologyImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            modelName: terminologyId
        )
        Terminology terminology = fhirTerminologyImporterProviderService.importDomain(admin, parameters)
        terminology.folder = folder
        terminologyService.validate(terminology)
        if (terminology.errors.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, terminology)
            throw new ValidationException("Domain object is not valid. Has ${terminology.errors.errorCount} errors", terminology.errors)
        }
        terminologyService.saveModelWithContent(terminology)
    }    
}