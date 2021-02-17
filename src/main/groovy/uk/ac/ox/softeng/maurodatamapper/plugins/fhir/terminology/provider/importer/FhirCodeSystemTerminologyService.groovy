package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyImporterProviderService

import org.springframework.beans.factory.annotation.Autowired

abstract class FhirCodeSystemTerminologyService<T extends FhirTerminologyImporterProviderServiceParameters>
    extends TerminologyImporterProviderService<T> {

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
