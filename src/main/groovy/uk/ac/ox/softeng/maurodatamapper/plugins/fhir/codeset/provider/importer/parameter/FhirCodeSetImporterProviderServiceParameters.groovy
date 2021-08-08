/*
 * Copyright 2020-2021 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter


import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetImporterProviderServiceParameters

class FhirCodeSetImporterProviderServiceParameters extends CodeSetImporterProviderServiceParameters {

    @ImportParameterConfig(
        displayName = 'FHIR Server Host',
        description = ['The FHIR host to import from. (e.g. https://fhir.hl7.org.uk)',
            'URL for importing will be {fhirHost}/{version}/ValueSet/{valueSetName}?',
            'If no version parameter is supplied then URL will be {fhirHost}/StructureDefinition/{structureDefinitionName}?'
        ],
        descriptionJoinDelimiter = ' ',
        order = 0,
        group = @ImportGroupConfig(
            name = 'FHIR Settings',
            order = -1
        ))
    String fhirHost

    @ImportParameterConfig(
        optional = true,
        displayName = 'FHIR Publication Version',
        description = ['The UK FHIR version to import. (e.g. STU3).',
            'If not provided then the "current" published version will be used.'],
        descriptionJoinDelimiter = ' ',
        order = 1,
        group = @ImportGroupConfig(
            name = 'FHIR Settings',
            order = -1
        ))
    String fhirVersion

    @ImportParameterConfig(
        optional = true,
        displayName = 'Value Set name',
        description = ['Name of the individual Value Set Resource to import.',
            'If this is left blank then all Value Sets will be imported for the defined version.'],
        descriptionJoinDelimiter = ' ',
        order = 2,
        group = @ImportGroupConfig(
            name = 'FHIR Settings',
            order = -1
        ))
    String modelName

    @ImportParameterConfig(
        hidden = true
    )
    Boolean finalised = false

    @ImportParameterConfig(
        hidden = true
    )
    String description

    @ImportParameterConfig(
        hidden = true
    )
    String author

    @ImportParameterConfig(
        hidden = true
    )
    String organisation

    @ImportParameterConfig(
        hidden = true
    )
    Boolean useDefaultAuthority = false
}
