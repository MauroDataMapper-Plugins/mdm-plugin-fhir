package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codesets.provider.importer

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.FhirCodeSetService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet

class FihrCodeSetImporterService extends FhirCodeSetService {

    @Autowired
    FHIRServerClient serverClient

    @Override
    Boolean canImportMultipleDomains() {
        return false
    }

    @Override
    CodeSet importCodeSet(User currentUser) {
        return null
    }

}
