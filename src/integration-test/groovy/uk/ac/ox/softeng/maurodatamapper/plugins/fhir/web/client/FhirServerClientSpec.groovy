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
