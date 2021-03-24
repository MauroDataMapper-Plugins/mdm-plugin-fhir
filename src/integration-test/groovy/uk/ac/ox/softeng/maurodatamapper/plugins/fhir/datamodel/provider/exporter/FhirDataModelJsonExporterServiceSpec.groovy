package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.exporter.FhirDataModelJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.FhirDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import com.google.common.base.CaseFormat
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
            GET("/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(0)
            }
            GET("/StructureDefinition") {
                query('_format', 'json')
                query('_summary', 'text')
                called(0)
            }
            GET("/StructureDefinition") {
                query('_format', 'json')
                query('_summary', 'count')
                called(0)
            }
        }
        def parameters = new FhirDataModelImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        //dataModel = turn Json into dataModel
        when:
        DataModel dataModel = fhirDataModelImporterProviderService.importModel(admin, parameters)
        then:
        dataModel
        dataModel.label == entryId

        //exportJSON = export dataModel into our JSON
        when:
        ByteArrayOutputStream exportedJsonBytes = (fhirDataModelJsonExporterService.exportDataModel(admin, dataModel))
        String exportedJson = new String(exportedJsonBytes.toByteArray())

        then:
        exportedJson
        validateExportedModel(entryId, exportedJson)
    }

    String loadJsonString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }

    void validateExportedModel(String entryId, String exportedModel) {
        assert exportedModel, 'There must be an exported model string'

        Path expectedPath = resourcesPath.resolve("${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, entryId)}_exported.json")
        if (!Files.exists(expectedPath)) {
            Files.write(expectedPath, exportedModel.bytes)
            Assert.fail("Expected export file ${expectedPath} does not exist")
        }

        String expectedJson = replaceContentWithMatchers(Files.readString(expectedPath))
        verifyJson(expectedJson, exportedModel)
    }
}

//dataModel reImportedDataModel = import dataModel(exportJSON)
//dataModelService.diff(exportedDataModel, reImportedDataModel)
//numberOfDiffs == 0