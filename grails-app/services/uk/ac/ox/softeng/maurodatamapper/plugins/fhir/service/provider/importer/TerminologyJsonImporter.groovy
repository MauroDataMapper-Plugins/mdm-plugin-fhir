package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.service.provider.importer

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.service.FhirCodeSystemTerminologyService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology

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
