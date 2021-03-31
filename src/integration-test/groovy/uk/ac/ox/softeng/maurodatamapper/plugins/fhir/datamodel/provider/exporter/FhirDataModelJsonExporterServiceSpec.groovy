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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.exporter.FhirDataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.FhirDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Integration
@Rollback
@Slf4j
class FhirDataModelJsonExporterServiceSpec extends BaseFunctionalSpec implements JsonComparer {

    FhirDataModelImporterProviderService fhirDataModelImporterProviderService
    FhirDataModelJsonExporterService fhirDataModelJsonExporterService
    DataModelService dataModelService

    @Shared
    Path resourcesPath

    @OnceBefore
    void setupServerClient() {
        resourcesPath =
            Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'structure_definitions').toAbsolutePath()
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

    def "CC01: verify exported DataModel JSON content - CareConnect-ProcedureRequest-1"() {
        //import FHIR Json file
        given:
        String entryId = 'CareConnect-ProcedureRequest-1'
        String exportedEntryId = "${entryId}_exported"
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
            GET("/$version/StructureDefinition/$exportedEntryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${exportedEntryId}.json"))
                }
            }
        }
        def parameters = new FhirDataModelImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        //dataModel = turn Json into dataModel
        when:
        DataModel imported = fhirDataModelImporterProviderService.importModel(admin, parameters)
        then:
        imported
        imported.label == entryId

        //exportJSON = export dataModel into our JSON
        when:
        ByteArrayOutputStream exportedJsonBytes = (fhirDataModelJsonExporterService.exportDataModel(admin, imported))
        String exportedJson = new String(exportedJsonBytes.toByteArray())

        then:
        exportedJson
        validateExportedModel(entryId, exportedJson)

        def reParameters = new FhirDataModelImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            //name has to be entryId or it flags that as a Diff
            modelName: entryId
        )

        when:
        DataModel reImported = fhirDataModelImporterProviderService.importModel(admin, reParameters)

        then:
        reImported

        when:
        ObjectDiff od = dataModelService.getDiffForModels(imported, reImported)

        then:
        od.getNumberOfDiffs() == 0
    }

    def "CC02: verify exported DataModel JSON content - CareConnect-OxygenSaturation-Observation-1"() {
        //import FHIR Json file
        given:
        String entryId = 'CareConnect-OxygenSaturation-Observation-1'
        String exportedEntryId = "${entryId}_exported"
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
            GET("/$version/StructureDefinition/$exportedEntryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${exportedEntryId}.json"))
                }
            }
        }
        def parameters = new FhirDataModelImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        //dataModel = turn Json into dataModel
        when:
        DataModel imported = fhirDataModelImporterProviderService.importModel(admin, parameters)
        then:
        imported
        imported.label == entryId

        //exportJSON = export dataModel into our JSON
        when:
        ByteArrayOutputStream exportedJsonBytes = (fhirDataModelJsonExporterService.exportDataModel(admin, imported))
        String exportedJson = new String(exportedJsonBytes.toByteArray())

        then:
        exportedJson
        validateExportedModel(entryId, exportedJson)

        def reParameters = new FhirDataModelImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when:
        DataModel reImported = fhirDataModelImporterProviderService.importModel(admin, reParameters)

        then:
        reImported

        when:
        ObjectDiff od = dataModelService.getDiffForModels(imported, reImported)

        then:
        od.getNumberOfDiffs() == 0
    }

    String loadJsonString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }

    void validateExportedModel(String entryId, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${entryId}_exported.json")
        if (!Files.exists(expectedPath)) {
            Files.write(expectedPath, exportedModel.bytes)
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }

        String expectedJson = replaceContentWithMatchers(Files.readString(expectedPath))
        verifyJson(expectedJson, exportedModel)
    }
}