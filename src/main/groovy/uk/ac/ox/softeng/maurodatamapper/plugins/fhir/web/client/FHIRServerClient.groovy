package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client

import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client


@Client("http://fhir.hl7.org.uk/STU3/")
@Header(name = "ContentType", value="application/fhir+json")
interface FHIRServerClient {

    @Get("CodeSystem/CareConnect-ConditionCategory-1?_format={format}")
    String getCodeSystemTerminologies(String format)

    @Get("StructureDefinition/CareConnect-AllergyIntolerance-1/_history/1.0?_format={format}")
    String getStructureDefinition(String format)

}
