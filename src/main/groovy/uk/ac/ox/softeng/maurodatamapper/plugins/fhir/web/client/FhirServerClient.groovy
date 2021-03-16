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
import io.micronaut.http.client.annotation.Client

@Client('http://fhir.hl7.org.uk/STU3/')
interface FhirServerClient {

    public static String FHIR_MEDIA_TYPE = 'application/json+fhir;charset=utf-8'

    @Get(value = 'StructureDefinition?_format=json&_count={count}', produces = [FHIR_MEDIA_TYPE], consumes = [FHIR_MEDIA_TYPE])
    Map<String, Object> getStructureDefinition(int count)

    @Get(value = 'StructureDefinition/{entryId}?_format=json', produces = [FHIR_MEDIA_TYPE], consumes = [FHIR_MEDIA_TYPE])
    Map<String, Object> getStructureDefinitionEntry(String entryId)
}
