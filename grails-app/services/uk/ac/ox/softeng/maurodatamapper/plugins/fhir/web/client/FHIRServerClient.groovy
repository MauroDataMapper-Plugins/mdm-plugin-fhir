package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client

import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client

@Client("https://fhir.hl7.org.uk/STU3/")
interface FHIRServerClient {

    @Get("/CareConnect-ConditionCategory-1?_format={format}")
    String getCodeSystems(String format)

    @Get("StructureDefinition/CareConnect-AllergyIntolerance-1/1.0?_format={format}")
    String getStructureDefinition(String format)

}
