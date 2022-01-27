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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.exporter.FhirTerminologyExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirTerminologyImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import com.stehno.ersatz.ErsatzServer
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
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
class FhirTerminologyExporterProviderServiceSpec extends BaseFunctionalSpec implements JsonComparer {

    FhirTerminologyImporterProviderService fhirTerminologyImporterProviderService
    FhirTerminologyExporterProviderService fhirTerminologyExporterProviderService
    TerminologyService terminologyService

    @Shared
    Path resourcesPath

    @Shared
    Path exportedResourcesPath

    def setupSpec() {
        resourcesPath =
            Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'code_systems').toAbsolutePath()
        exportedResourcesPath =
            Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'code_systems', 'exported')
                .toAbsolutePath()

        ersatz = new ErsatzServer()
    }

    @Override
    String getResourcePath() {
        ''
    }

    @Shared
    ErsatzServer ersatz

    void cleanup() {
        ersatz.clearExpectations()
    }

    void cleanupSpec() {
        ersatz.stop()
    }

    @Unroll
    def 'CC01: verify exported Terminology JSON content - "#entryId"'() {
        //import FHIR Json file
        given:
        String exportedEntryId = "${entryId}_exported"
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/CodeSystem/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
            GET("/$version/CodeSystem/$exportedEntryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadExportedJsonString("${exportedEntryId}.json"))
                }
            }
        }
        def parameters = new FhirTerminologyImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when: 'the Terminology is imported from Json'
        Terminology imported = fhirTerminologyImporterProviderService.importDomain(admin, parameters)

        then: 'it is imported with the correct label'
        imported
        imported.label == entryId

        when: 'the imported Terminology is exported'
        ByteArrayOutputStream exportedJsonBytes = (fhirTerminologyExporterProviderService.exportTerminology(admin, imported))
        String exportedJson = new String(exportedJsonBytes.toByteArray())

        then: 'the exported Json is correct'
        exportedJson
        validateExportedModel(entryId, exportedJson)


        def reParameters = new FhirTerminologyImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: exportedEntryId
        )

        when: 'the terminology is reimported from the export'
        Terminology reImported = fhirTerminologyImporterProviderService.importDomain(admin, reParameters)

        then:
        reImported

        when: 'differences between the import and reimport are determined'
        ObjectDiff od = terminologyService.getDiffForModels(imported, reImported)

        then: 'there are no differences'
        od.getNumberOfDiffs() == 0

        where:
        entryId << [
            'CareConnect-ConditionCategory-1',
            'CareConnect-EthnicCategory-1',
            'CareConnect-HumanLanguage-1'
        ]
    }

    String loadJsonString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }

    String loadExportedJsonString(String filename) {
        Path testFilePath = exportedResourcesPath.resolve("${filename}").toAbsolutePath()
        Files.exists(testFilePath) ? Files.readString(testFilePath) : ''
    }

    void validateExportedModel(String entryId, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = exportedResourcesPath.resolve("${entryId}_exported.json")
        if (!Files.exists(expectedPath)) {
            Files.write(expectedPath, exportedModel.bytes)
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }

        String expectedJson = replaceContentWithMatchers(Files.readString(expectedPath))
        verifyJson(expectedJson, exportedModel)
    }
}