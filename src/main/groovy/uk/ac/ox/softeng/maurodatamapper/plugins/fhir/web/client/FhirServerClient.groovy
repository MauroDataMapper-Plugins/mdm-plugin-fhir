package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client


import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client

@Client('http://fhir.hl7.org.uk/STU3/')
interface FhirServerClient {

    public static String FHIR_MEDIA_TYPE = 'application/json+fhir;charset=utf-8'

    @Get(value = 'StructureDefinition?_format=json&_count={count}', produces = [FHIR_MEDIA_TYPE], consumes = [FHIR_MEDIA_TYPE])
    Map<String, Object> getStructureDefinition(int count)

    @Get(value = 'StructureDefinition/{entryId}?_format=json', produces = [FHIR_MEDIA_TYPE], consumes = [FHIR_MEDIA_TYPE])
    Map<String, Object> getStructureDefinitionEntry(String entryId)
}
