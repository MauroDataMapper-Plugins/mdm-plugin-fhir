package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.codset.provider.importer

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.FhirCodeSetService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter.FhirCodeSetImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirCodeSystemTerminologyService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

@Slf4j
@Integration
class FhirCodsetImportServiceSpec extends BaseIntegrationSpec {

    @Autowired
    FhirCodeSetService fhirCodeSetService

    @Override
    void setupDomainData() {
    }

    def "verify Code CodeSets"() {
        given:
        String entryId = 'NHS Data Model and Dictionary Mapping'
        new FhirCodeSetImporterProviderServiceParameters(
                modelName: entryId
        )

        when:
        def imported = fhirCodeSetService.importCodeSet(admin)

        then:
        imported
        imported.label == entryId
    }
}