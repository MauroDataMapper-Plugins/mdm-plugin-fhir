package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter.FhirCodeSetImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.CodeSetImporterProviderService

abstract class FhirCodeSetService<T extends FhirCodeSetImporterProviderServiceParameters> extends
        CodeSetImporterProviderService<T> {

    @Autowired
    FihrCodeSetImporterService jsonImporter

    abstract CodeSet importCodeSet(User currentUser, T params)

    CodeSet importModel(User user, T params) {
        jsonImporter.importCodeSet(user)
    }

    List<CodeSet> importModels(User user, T params) {
        try {
            return jsonImporter.importCodeSet(user)
        }
        catch (Exception e) {}
    }

    @Override
    String getDisplayName() {
        'FHIR Server CodeSet Importer'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

}
