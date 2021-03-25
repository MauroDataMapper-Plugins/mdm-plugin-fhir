package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Shared
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Integration
@Rollback
class FhirTerminologyImportServiceSpec extends BaseIntegrationSpec {

    @Autowired
    FhirCodeSystemTerminologyService fhirCodeSystemTerminologyService

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

    def "verify Terminology "() {
        given:
        setupDomainData()
        String entryId = 'Care Connect Condition Category'
        def parameters = new FhirTerminologyImporterProviderServiceParameters(
                modelName: entryId,
        )
        parameters.endpoint = "STU3/CodeSystem/CareConnect-ConditionCategory-1/_history/1.0?_format="
        when:
        def terminology = fhirCodeSystemTerminologyService.importTerminology(admin, parameters)

        then:
        !terminology.id

        when:
        terminology.folder = folder
        check(terminology)

        then:
        !terminology.hasErrors()

        when:
        Terminology saved = terminologyService.saveModelWithContent(terminology)

        then:
        saved.id
        saved.label == 'Care Connect Condition Category'

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