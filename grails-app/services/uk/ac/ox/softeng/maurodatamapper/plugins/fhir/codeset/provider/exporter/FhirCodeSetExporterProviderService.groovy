/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.exporter.CodeSetExporterProviderService

import grails.plugin.json.view.JsonViewTemplateEngine
import groovy.text.Template
import org.springframework.beans.factory.annotation.Autowired

class FhirCodeSetExporterProviderService extends CodeSetExporterProviderService {

    public static final String CONTENT_TYPE = 'application/fhir+json'

    @Autowired
    JsonViewTemplateEngine templateEngine

    @Override
    String getFileExtension() {
        'json'
    }

    @Override
    String getContentType() {
        CONTENT_TYPE
    }

    @Override
    String getDisplayName() {
        'FHIR ValueSet (CodeSet) JSON Exporter'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    String getNamespace() {
        'uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset'
    }

    @Override
    ByteArrayOutputStream exportCodeSet(User currentUser, CodeSet codeSet, Map<String, Object> parameters) throws ApiException {
        exportModel(codeSet, contentType)
    }

    ByteArrayOutputStream exportModel(CodeSet codeSet, String format) {
        Template template = templateEngine.resolveTemplate(exportViewPath)

        if (!template) {
            log.error('Could not find template for format {} at path {}', format, exportViewPath)
            throw new ApiInternalException('CSE02', "Could not find template for format ${format} at path ${exportViewPath}")
        }

        def writable = template.make(codeSet: codeSet)
        def sw = new StringWriter()
        writable.writeTo(sw)
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        os.write(sw.toString().bytes)
        os
    }

    @Override
    ByteArrayOutputStream exportCodeSets(User currentUser, List<CodeSet> codeSets, Map<String, Object> parameters) throws ApiException {
        throw new ApiBadRequestException('CSE01', "${getName()} cannot export multiple CodeSets")
    }

    static String getExportViewPath() {
        '/valueSet/export'
    }
}
