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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyImporterProviderService

import org.springframework.beans.factory.annotation.Autowired

abstract class FhirCodeSystemTerminologyService<T extends FhirTerminologyImporterProviderServiceParameters>
    extends TerminologyImporterProviderService<T> {

    @Autowired
    FhirTerminologyImporterService jsonImporter

    abstract Terminology importTerminology(User currentUser, T params)

    Terminology importModel(User user, T params) {
        jsonImporter.importTerminology(user, params)
    }

    List<Terminology> importModels(User user, T params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        if (params.modelName) {
            log.debug('Model name supplied, only importing 1 model')
            return [importModel(user, params)]
        }

        try {
            return jsonImporter.importTerminology(user)
        }
        catch (Exception e) {}
    }

    @Override
    String getDisplayName() {
        'FHIR Server Terminology Importer'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }
}
