package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig

class FhirCodeSetImporterProviderServiceParameters extends ModelImporterProviderServiceParameters {
    @ImportParameterConfig(
            displayName = 'Category',
            description = 'Terminology data Category',
            order = -1,
            group = @ImportGroupConfig(
                    name = 'FHIR terminology Data',
                    order = 1
            )
    )
    String category

    @ImportParameterConfig(
            displayName = 'Version',
            description = 'Terminology data Version',
            order = -1,
            group = @ImportGroupConfig(
                    name = 'FHIR terminology Data Version',
                    order = 2
            )
    )
    String version


}
