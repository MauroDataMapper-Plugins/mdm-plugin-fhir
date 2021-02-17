package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.service


import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.service.provider.importer.TerminologyJsonImporter
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyImporterProviderService

abstract class FhirCodeSystemTerminologyService<T extends FhirTerminologyImporterProviderServiceParameters>
        extends TerminologyImporterProviderService<T>{

    @Autowired
    TerminologyJsonImporter jsonImporter

    abstract Terminology importTerminology(User currentUser)

    Terminology importModel(User user, T params) {
        jsonImporter.importTerminology(user)
    }

    List<Terminology> importModels(User user, T params) {
        try {
            jsonImporter.importTerminology(user)
        }
        catch (Exception e) {}

        return null
    }

    @Override
    String getDisplayName() {
        'FHIR Server Terminology Importer'
    }

    @Override
    String getVersion() {
        '1.0.0-SNAPSHOT'
    }
}
