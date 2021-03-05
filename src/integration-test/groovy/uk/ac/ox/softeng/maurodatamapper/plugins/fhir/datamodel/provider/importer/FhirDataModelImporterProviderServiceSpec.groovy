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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer


import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.FhirDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
class FhirDataModelImporterProviderServiceSpec extends BaseIntegrationSpec {

    FhirDataModelImporterProviderService fhirDataModelImporterProviderService

    @Override
    void setupDomainData() {
    }

    def 'Test importing single datamodel'() {
        given:
        String entryId = 'CareConnect-Condition-1'
        def parameters = new FhirDataModelImporterProviderServiceParameters(
            modelName: entryId
        )

        when:
        DataModel imported = fhirDataModelImporterProviderService.importModel(admin, parameters)

        then:
        imported
        imported.label == entryId
        assert 'more tests' == 'true'
    }

    def 'Test importing multiple datamodel'() {
        given:
        def parameters = new FhirDataModelImporterProviderServiceParameters()

        when:
        List<DataModel> imported = fhirDataModelImporterProviderService.importModels(admin, parameters)

        then:
        imported
        // STU3 has 111 models
        imported.size() == 111
        assert 'more tests' == 'true'
    }
}
