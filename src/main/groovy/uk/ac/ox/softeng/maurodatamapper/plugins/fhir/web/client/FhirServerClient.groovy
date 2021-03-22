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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.DefaultHttpClient
import io.micronaut.http.client.DefaultHttpClientConfiguration
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.uri.UriBuilder
import io.reactivex.Flowable

import java.time.Duration
import javax.inject.Inject

/**
 * RESTful API Client connection to FHIR server.
 *
 * See https://www.hl7.org/fhir/http.html for complete documentation on the API.
 *
 * Notable parameters:
 * <pre>
 * * _format=json :: Ensures JSON returned
 * * _count=x :: Paginates the call to x entries
 * * _summary=text :: Returns a text only summary of the endpoint. Especially useful for the "list all models" endpoints.
 * * _summary=count :: Returns a count only summary of the endpoint. Especially useful for the first "list all models" endpoints.
 * </pre
 */
@Slf4j
class FhirServerClient {

    private HttpClient client

    GrailsApplication grailsApplication

    FhirServerClient(String hostUrl) {
        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration()
        client = new DefaultHttpClient(hostUrl.toURL(), configuration)
        log.debug('Client created to connect to {}', hostUrl)
    }

    Map<String, Object> getVersionedStructureDefinition(String version, int count) {
        if (!version) return getCurrentStructureDefinition(count)
        retrieveMapFromClient('/{version}/StructureDefinition?_summary=text&_format=json&_count={count}',
                [version: version,
                 count  : count])
    }

    Map<String, Object> getVersionedStructureDefinitionCount(String version) {
        if (!version) return getCurrentStructureDefinitionCount()
        retrieveMapFromClient('/{version}/StructureDefinition?_summary=count&_format=json', [version: version])
    }

    Map<String, Object> getVersionedStructureDefinitionEntry(String version, String entryId) {
        if (!version) return getCurrentStructureDefinitionEntry(entryId)
        retrieveMapFromClient('/{version}/StructureDefinition/{entryId}?_format=json',
                [version: version,
                 entryId: entryId])
    }

    Map<String, Object> getCurrentStructureDefinition(int count) {
        retrieveMapFromClient('/StructureDefinition?_summary=text&_format=json&_count={count}', [count: count])
    }

    Map<String, Object> getCurrentStructureDefinitionCount() {
        retrieveMapFromClient('/StructureDefinition?_summary=count&_format=json', [:])
    }

    Map<String, Object> getCurrentStructureDefinitionEntry(String entryId) {
        retrieveMapFromClient('/StructureDefinition/{entryId}?_format=json', [entryId: entryId])
    }

    private Map<String, Object> retrieveMapFromClient(String url, Map params) {
        try {
            Flowable<Map> response = client.retrieve(HttpRequest.GET(UriBuilder.of
            (url).expand(params)), Argument.of(Map, String, Object)) as Flowable<Map>
            response.blockingFirst()
        }
        catch (HttpException ex) {
            throw new ApiInternalException('FHIRC01', "Could not load resource from endpoint [${url}]", ex)
        }
    }
}
