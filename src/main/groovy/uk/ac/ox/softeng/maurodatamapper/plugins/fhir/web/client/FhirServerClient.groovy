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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException

import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.AnnotationMetadataResolver
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.DefaultHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.LoadBalancer
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.client.ssl.NettyClientSslBuilder
import io.micronaut.http.codec.MediaTypeCodecRegistry
import io.micronaut.http.exceptions.HttpException
import io.micronaut.http.uri.UriBuilder
import io.netty.channel.MultithreadEventLoopGroup
import io.netty.util.concurrent.DefaultThreadFactory
import io.reactivex.Flowable
import org.springframework.context.ApplicationContext

import java.util.concurrent.ThreadFactory

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
    private String hostUrl
    private String versionPath

    FhirServerClient(String hostUrl, ApplicationContext applicationContext) {
        this(hostUrl, null,
             applicationContext.getBean(HttpClientConfiguration),
             new DefaultThreadFactory(MultithreadEventLoopGroup),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry)
        )
    }

    FhirServerClient(String hostUrl, String versionPath, ApplicationContext applicationContext) {
        this(hostUrl, versionPath,
             applicationContext.getBean(HttpClientConfiguration),
             new DefaultThreadFactory(MultithreadEventLoopGroup),
             applicationContext.getBean(NettyClientSslBuilder),
             applicationContext.getBean(MediaTypeCodecRegistry)
        )
    }

    FhirServerClient(String hostUrl, String versionPath,
                     HttpClientConfiguration httpClientConfiguration,
                     ThreadFactory threadFactory,
                     NettyClientSslBuilder nettyClientSslBuilder,
                     MediaTypeCodecRegistry mediaTypeCodecRegistry) {
        this.hostUrl = hostUrl
        this.versionPath = versionPath
        client = new DefaultHttpClient(LoadBalancer.fixed(hostUrl.toURL()),
                                       httpClientConfiguration,
                                       versionPath,
                                       threadFactory,
                                       nettyClientSslBuilder,
                                       mediaTypeCodecRegistry,
                                       AnnotationMetadataResolver.DEFAULT)
        log.debug('Client created to connect to {}', hostUrl)
    }

    Map<String, Object> getStructureDefinitions(int count) {
        retrieveMapFromClient('/StructureDefinition?_summary=text&_format=json&_count={count}', [count: count])
    }

    Map<String, Object> getStructureDefinitionCount() {
        retrieveMapFromClient('/StructureDefinition?_summary=count&_format=json', [:])
    }

    Map<String, Object> getStructureDefinitionEntry(String entryId) {
        retrieveMapFromClient('/StructureDefinition/{entryId}?_format=json', [entryId: entryId])
    }

    Map<String, Object> getValueSets(int count) {
        retrieveMapFromClient('/ValueSet?_summary=text&_format=json&_count={count}', [count: count])
    }

    Map<String, Object> getValueSetCount() {
        retrieveMapFromClient('/ValueSet?_summary=count&_format=json', [:])
    }

    Map<String, Object> getValueSetEntry(String entryId) {
        retrieveMapFromClient('/ValueSet/{entryId}?_format=json', [entryId: entryId])
    }

    Map<String, Object> getCodeSystems(int count) {
        retrieveMapFromClient('/CodeSystem?_summary=text&_format=json&_count={count}', [count: count])
    }

    Map<String, Object> getCodeSystemCount() {
        retrieveMapFromClient('/CodeSystem?_summary=count&_format=json', [:])
    }

    Map<String, Object> getCodeSystemEntry(String entryId) {
        retrieveMapFromClient('/CodeSystem/{entryId}?_format=json', [entryId: entryId])
    }

    URI getHostUri() {
        UriBuilder.of(hostUrl).build()
    }

    private Map<String, Object> retrieveMapFromClient(String url, Map params) {
        try {
            Flowable<Map> response = client.retrieve(HttpRequest.GET(UriBuilder.of(url)
                                                                         .expand(params)), Argument.of(Map, String, Object)) as Flowable<Map>
            response.blockingFirst()
        }
        catch (HttpClientResponseException responseException) {
            String fullUrl = UriBuilder.of(hostUrl).path(versionPath).path(url).expand(params).toString()
            if (responseException.status == HttpStatus.NOT_FOUND) {
                throw new ApiBadRequestException('FHIRC01', "Requested endpoint could not be found ${fullUrl}")
            }
            Map responseBody = extractExceptionBody(responseException)
            List issues = responseBody.issue
            if (issues) {
                if (issues.first().diagnostics == 'Failed to call access method') {
                    throw new ApiBadRequestException('FHIRC01', "Requested endpoint could not be found ${fullUrl}")
                }
            }
            throw new ApiInternalException('FHIRC02', "Could not load resource from endpoint [${fullUrl}].\n" +
                                                      "Response body [${responseException.response.body()}]",
                                           responseException)
        } catch (HttpException ex) {
            String fullUrl = UriBuilder.of(hostUrl).path(versionPath).path(url).expand(params).toString()
            throw new ApiInternalException('FHIRC03', "Could not load resource from endpoint [${fullUrl}]", ex)
        }
    }

    private static Map extractExceptionBody(HttpClientResponseException responseException) {
        try {
            responseException.response.body() as Map
        } catch (Exception ignored) {
            [:]
        }

    }
}
