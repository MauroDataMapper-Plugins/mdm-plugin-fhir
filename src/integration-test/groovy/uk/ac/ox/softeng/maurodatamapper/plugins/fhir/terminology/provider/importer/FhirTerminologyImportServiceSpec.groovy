package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
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
class FhirTerminologyImportServiceSpec extends BaseFunctionalSpec {

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
        TerminologyService terminologyService = Mock()
        FhirServerClient serverClient = Stub(FhirServerClient) {
            it.getCodeSystemTerminologies('json') >> fileAsString
        }
        FhirCodeSystemTerminologyService fhir = new FihrTerminologyImporterService()
        def parameters = new FhirTerminologyImporterProviderServiceParameters()

        given:
        fhir.serverClient = serverClient
        fhir.terminologyService = terminologyService

        when:
        def terminology = fhir.importTerminology(admin)

        then:
        1 * terminologyService._
        assert(terminology.label =='Care Connect Condition Category')
        assert(terminology.terms.getAt(0).label =='problem-list-item: An item on a problem list which can be managed over time and can be expressed by a practitioner (e.g. physician, nurse), patient, or related person.')
    }


    @Override
    String getResourcePath() {
        ''
    }
}