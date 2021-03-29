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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
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
        jsonImporter.importCodeSet(user, params)
    }

    List<CodeSet> importModels(User user, T params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        if (params.modelName) {
            log.debug('Model name supplied, only importing 1 model')
            return [importModel(user, params)]
        }

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
