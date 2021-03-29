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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

/**
 * This test requires a live connection to the internet and for the server https://fhir.hl7.org.uk to be live
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

    void 'GEN-01: Test the structure definition entry id for non-existent entry'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)
        String entryId = 'non-existent-entry'

        when:
        fhirServerClient.getStructureDefinitionEntry(entryId)

        then:
        ApiBadRequestException ex = thrown(ApiBadRequestException)
        ex.message == 'Requested endpoint could not be found https://fhir.hl7.org.uk/STU3/StructureDefinition/non-existent-entry?_format=json'
    }

    void 'STU3-04: Test the value set count endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)

        when:
        def result = fhirServerClient.getValueSetCount()

        then:
        result instanceof Map
        result.total == 75
        result.resourceType == 'Bundle'
        result.id
    }

    void 'STU3-05: Test the value set summary endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)

        when:
        def result = fhirServerClient.getValueSets(2)

        then:
        result instanceof Map
        result.total == 75
        result.entry instanceof List
        result.entry.size() == 2
        result.entry.first().resource.id
    }

    void 'STU3-06: Test the value set entry endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)
        String entryId = 'CareConnect-AdministrativeGender-1'

        when:
        def result = fhirServerClient.getValueSetEntry(entryId)

        then:
        result instanceof Map
        result.id == entryId
        result.resourceType == 'ValueSet'
        result.name == 'Administrative Gender'

    }

    void 'PUB-04: Test the value set count endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)

        when:
        def result = fhirServerClient.getValueSetCount()

        then:
        result instanceof Map
        result.total == 28
        result.resourceType == 'Bundle'
        result.id
    }

    void 'PUB-05: Test the value set summary endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)

        when:
        def result = fhirServerClient.getValueSets(2)

        then:
        result instanceof Map
        result.total == 28
        result.entry instanceof List
        result.entry.size() == 2
        result.entry.first().resource.id
    }

    void 'PUB-06: Test the value set entry endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)
        String entryId = 'CareConnect-AdministrativeGender-1'

        when:
        def result = fhirServerClient.getValueSetEntry(entryId)

        then:
        result instanceof Map
        result.id == entryId
        result.resourceType == 'ValueSet'
        result.name == 'Care Connect Administrative Gender'

    }

    void 'VER-02: Test the value set count endpoint with a blank version'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', '', applicationContext)

        when:
        def result = fhirServerClient.getValueSetCount()

        then:
        result instanceof Map
        result.total == 28
        result.resourceType == 'Bundle'
        result.id
    }

    void 'GEN-02: Test the value set entry id for non-existent entry'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)
        String entryId = 'non-existent-entry'

        when:
        fhirServerClient.getValueSetEntry(entryId)

        then:
        ApiBadRequestException ex = thrown(ApiBadRequestException)
        ex.message == 'Requested endpoint could not be found https://fhir.hl7.org.uk/STU3/ValueSet/non-existent-entry?_format=json'
    }

    void 'STU3-07: Test the code system count endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)

        when:
        def result = fhirServerClient.getCodeSystemCount()

        then:
        result instanceof Map
        result.total == 36
        result.resourceType == 'Bundle'
        result.id
    }

    void 'STU3-08: Test the code system summary endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)

        when:
        def result = fhirServerClient.getCodeSystems(2)

        then:
        result instanceof Map
        result.total == 36
        result.entry instanceof List
        result.entry.size() == 2
        result.entry.first().resource.id
    }

    void 'STU3-09: Test the code system entry endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)
        String entryId = 'CareConnect-ConditionCategory-1'

        when:
        def result = fhirServerClient.getCodeSystemEntry(entryId)

        then:
        result instanceof Map
        result.id == entryId
        result.resourceType == 'CodeSystem'
        result.name == 'Care Connect Condition Category'

    }

    void 'PUB-07: Test the code system count endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)

        when:
        fhirServerClient.getCodeSystemCount()

        then: 'Only STU3 currently supports CodeSystem'
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'FHIRC01'
        exception.message.startsWith('Requested endpoint could not be found')
    }

    void 'PUB-08: Test the code system summary endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)

        when:
        fhirServerClient.getCodeSystems(2)

        then: 'Only STU3 currently supports CodeSystem'
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'FHIRC01'
        exception.message.startsWith('Requested endpoint could not be found')
    }

    void 'PUB-09: Test the code system entry endpoint'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', applicationContext)
        String entryId = 'CareConnect-AdministrativeGender-1'

        when:
        fhirServerClient.getCodeSystemEntry(entryId)

        then: 'Only STU3 currently supports CodeSystem'
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'FHIRC01'
        exception.message.startsWith('Requested endpoint could not be found')

    }

    void 'VER-03: Test the code system count endpoint with a blank version'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', '', applicationContext)

        when:
        fhirServerClient.getCodeSystemCount()

        then: 'Only STU3 currently supports CodeSystem'
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'FHIRC01'
        exception.message.startsWith('Requested endpoint could not be found')
    }

    void 'GEN-03: Test the code system entry id for non-existent entry'() {
        given:
        fhirServerClient = new FhirServerClient('https://fhir.hl7.org.uk', 'STU3', applicationContext)
        String entryId = 'non-existent-entry'

        when:
        fhirServerClient.getCodeSystemEntry(entryId)

        then:
        ApiBadRequestException ex = thrown(ApiBadRequestException)
        ex.message == 'Requested endpoint could not be found https://fhir.hl7.org.uk/STU3/CodeSystem/non-existent-entry?_format=json'
    }
}
