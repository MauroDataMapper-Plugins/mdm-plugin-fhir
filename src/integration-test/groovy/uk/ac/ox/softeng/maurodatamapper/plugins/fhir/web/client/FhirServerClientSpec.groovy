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
import org.springframework.context.ApplicationContext

/**
 * @since 05/03/2021
 */
@Slf4j
@Integration
class FhirServerClientSpec extends BaseIntegrationSpec {

    FhirServerClient fhirServerClient

    @Autowired
    ApplicationContext applicationContext

    @Override
    void setupDomainData() {
    }


    void 'STU3-01: Test the structure definition count endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)

        when:
        def result = fhirServerClient.getStructureDefinitionCount()

        then:
        result instanceof Map
        result.total == 111
        result.resourceType == 'Bundle'
        result.id
    }

    void 'STU3-02: Test the structure definition summary endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)

        when:
        def result = fhirServerClient.getStructureDefinitions(2)

        then:
        result instanceof Map
        result.total == 111
        result.entry instanceof List
        result.entry.size() == 2
        result.entry.first().resource.id
    }

    void 'STU3-03: Test the structure definition entry endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)
        String entryId = 'CareConnect-Condition-1'

        when:
        def result = fhirServerClient.getStructureDefinitionEntry(entryId)

        then:
        result instanceof Map
        result.id == entryId
        result.resourceType == 'StructureDefinition'
        result.name == entryId

    }

    void 'PUB-01: Test the structure definition count endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)

        when:
        def result = fhirServerClient.getStructureDefinitionCount()

        then:
        result instanceof Map
        result.total == 51
        result.resourceType == 'Bundle'
        result.id
    }

    void 'PUB-02: Test the structure definition summary endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)

        when:
        def result = fhirServerClient.getStructureDefinitions(2)

        then:
        result instanceof Map
        result.total == 51
        result.entry instanceof List
        result.entry.size() == 2
        result.entry.first().resource.id
    }

    void 'PUB-03: Test the structure definition entry endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)
        String entryId = 'CareConnect-Condition-1'

        when:
        def result = fhirServerClient.getStructureDefinitionEntry(entryId)

        then:
        result instanceof Map
        result.id == entryId
        result.resourceType == 'StructureDefinition'
        result.name == entryId

    }

    void 'VER-01: Test the structure definition count endpoint with a blank version'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', '', applicationContext)

        when:
        def result = fhirServerClient.getStructureDefinitionCount()

        then:
        result instanceof Map
        result.total == 51
        result.resourceType == 'Bundle'
        result.id
    }
}
