/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter

import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportGroupConfig
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelImporterProviderServiceParameters

class FhirDataModelImporterProviderServiceParameters extends DataModelImporterProviderServiceParameters {

    @ImportParameterConfig(
        optional = true,
        displayName = 'Structure Definition name',
        description = ['Name of the individual Structure Definition Resource to import.',
            'If this is left blank then all Structure Definitions will be imported from the FHIR endpoint.'],
        descriptionJoinDelimiter = ' ',
        order = 3,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    String modelName

    @ImportParameterConfig(
        optional = true,
        displayName = 'FHIR Publication Version',
        description = ['The FHIR version to import, see http://hl7.org/fhir/directory.html for all versions.',
            'e.g. STU3 or R4 or 2020Sep',
            'If not provided then the "current" published version will be used.'],
        descriptionJoinDelimiter = ' ',
        order = 3,
        group = @ImportGroupConfig(
            name = 'Model',
            order = 0
        ))
    String fhirVersion
}
