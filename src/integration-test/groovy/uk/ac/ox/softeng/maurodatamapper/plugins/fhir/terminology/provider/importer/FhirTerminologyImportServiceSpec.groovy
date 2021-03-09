package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer

import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.FhirDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.TestUser

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Integration
class FhirTerminologyImportServiceSpec extends BaseIntegrationSpec {

    @Autowired
    FhirCodeSystemTerminologyService fhirCodeSystemTerminologyService

    @Override
    void setupDomainData() {
    }

    def "verify Code system terminology"() {
        given:
        String entryId = 'Care Connect Condition Category'
        new FhirTerminologyImporterProviderServiceParameters(
                modelName: entryId
        )

        when:
        def imported = fhirCodeSystemTerminologyService.importTerminology(admin)

        then:
        imported
        imported.label == entryId
    }
}