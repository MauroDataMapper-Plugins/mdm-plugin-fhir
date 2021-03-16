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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client

import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 05/03/2021
 */
@Slf4j
@Integration
class FhirServerClientSpec extends BaseIntegrationSpec {

    @Autowired
    FhirServerClient fhirServerClient

    @Override
    void setupDomainData() {
    }

    void 'Test the structure definition endpoint'() {
        when:
        def result = fhirServerClient.getStructureDefinition(1)

        then:
        result instanceof Map
        result.total == 111
        result.entry instanceof List
        result.entry.size() == 1
        result.entry.first().resource.id
    }

    void 'Test the structure definition entry endpoint'() {
        given:
        String entryId = 'CareConnect-Condition-1'

        when:
        def result = fhirServerClient.getStructureDefinitionEntry(entryId)

        then:
        result instanceof Map
        result.id == entryId
        result.resourceType == 'StructureDefinition'
        result.name == entryId

    }
}
