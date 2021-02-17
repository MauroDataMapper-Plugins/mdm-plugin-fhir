package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirCodeSystemTerminologyService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired

// This class needs to be renamed and as the service it will be the one which appears in the UI, it should be appropriatelt named such as
// FihrTerminologyImporterService is what this should be called, and should ideally extend TerminologyImporterProviderService unless youplan on
// having multiple forms of terminology importing in which case you should probably have a base abstract class like the one you have and then multiple
// classes which extend this and are called FihrXxxxxTerminologyImporterService
// Also the classname MUST end in Service
class TerminologyJsonImporter extends FhirCodeSystemTerminologyService {

    @Autowired
    FHIRServerClient serverClient

    @Override
    Terminology importTerminology(User currentUser) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        log.debug("importCodSets")

        def codeSystems = serverClient.getCodeSystemTerminologies('json')
        def response = new JsonSlurper().parseText(codeSystems)
        return null
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }
}
