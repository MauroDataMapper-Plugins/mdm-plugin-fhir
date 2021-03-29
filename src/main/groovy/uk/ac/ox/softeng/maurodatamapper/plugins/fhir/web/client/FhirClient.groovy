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
