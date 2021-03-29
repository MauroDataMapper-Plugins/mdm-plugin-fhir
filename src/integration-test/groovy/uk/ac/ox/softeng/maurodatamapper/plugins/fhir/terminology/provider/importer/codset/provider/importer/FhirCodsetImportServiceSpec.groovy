package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.codset.provider.importer

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.FihrCodeSetImporterService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirTerminologyImporterService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Integration
@Rollback
class FhirCodsetImportServiceSpec extends BaseIntegrationSpec {

    @Autowired
    FhirTerminologyImporterService fhirTerminologyImporterService

    @Autowired
    FihrCodeSetImporterService fihrCodeSetImporterService

    CodeSetService codeSetService

    TerminologyService terminologyService

    @Shared
    Path resourcesPath

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources').toAbsolutePath()
    }

    @Override
    void setupDomainData() {

        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)
    }

    byte[] loadTestFile(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    def "verify CodeSets"() {
        given:
        setupDomainData()

        String entryId = 'Care Connect Condition Category'
        def param = new FhirTerminologyImporterProviderServiceParameters(
                modelName: entryId,
        )

        Map terminologies = new JsonSlurper().parseText(new String(loadTestFile("fhir-server-code-systems-payload.json"), Charset.defaultCharset()))
        Map codeSetMap = new JsonSlurper().parseText(new String(loadTestFile("fhir-server-value-set-payload.json"), Charset.defaultCharset()))

        when:
        def terminology = fhirTerminologyImporterService.bindMapToTerminology(admin, terminologies)
        terminology.folder = folder
        check(terminology)

        Terminology saved = terminologyService.saveModelWithContent(terminology)
        saved.id
        saved.label == 'Care Connect Condition Category'

        def codeSet = fihrCodeSetImporterService.bindMapToCodeSet(admin, codeSetMap as HashMap)
        codeSet.folder = folder
        CodeSet savedCodeSet = codeSetService.saveModelWithContent(codeSet)

        then:
        savedCodeSet.id
        savedCodeSet.label == 'Care Connect Condition Category'
    }
}