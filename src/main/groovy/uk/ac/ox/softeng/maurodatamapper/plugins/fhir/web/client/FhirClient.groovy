package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client

import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client


@Client("http://fhir.hl7.org.uk/")
@Header(name = "ContentType", value="application/fhir+json")
interface FhirClient {
    @Get("STU3/CodeSystem/{category}/_history/{version}?_format={format}")
    String getCodeSystemTerminologies(String category, String version, String format)

    @Get("ValueSet/{category}/_history/{version}/_history/{version}?_format={format}")
    String getCodeSets(String category, String version, String format)

}
