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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer

import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.FhirCodeSetService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter.FhirCodeSetImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

class FihrCodeSetImporterService extends FhirCodeSetService {

    public static List<String> NON_METADATA_KEYS = ['concept', "codeSystem"]
    @Autowired
    FhirClient serverClient

    @Autowired
    TerminologyService terminologyService

    @Override
    Boolean canImportMultipleDomains() {
        return false
    }

  //  @Override
    CodeSet importCodeSet(User currentUser, FhirCodeSetImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR02', 'User must be logged in to import model')
        if (!params.category) throw new ApiUnauthorizedException('FHIR02', 'Category cannot be null')
        if (!params.version) throw new ApiUnauthorizedException('FHIR02', 'Version cannot be null')
        log.debug("importCodSets")
        def category = params.category.toString()
        def version = params.version.toString()

        def codeSets = serverClient.getCodeSets(category, version, 'json')
        Map codeSetMap = new JsonSlurper().parseText(codeSets)
        bindMapToCodeSet(currentUser, new HashMap(codeSetMap))
    }

    CodeSet bindMapToCodeSet(User user, HashMap codeSetMap) {
        if (!codeSetMap) throw new ApiBadRequestException('FHIR04', 'No codeSetMap supplied to import')

        def terminologies = terminologyService.findAllByLabel(codeSetMap.name)
        def codeSet = new CodeSet()
        Map codeSystem = codeSetMap.codeSystem
        Map conceptProp = codeSetMap.concept
        def concepts
        if (codeSystem) {
            concepts = codeSystem.concept
        } else if (conceptProp) {
            concepts = conceptProp
        }

        concepts.each { concept ->
            terminologies.each { terminology ->
                terminology.terms.each { term ->
                    Term codeSetTerm = new Term()
                    codeSetTerm.label = !term.label ? "" : term.label
                    codeSetTerm.code = !term.code ? "" : term.code
                    codeSetTerm.definition = !term.definition ? "" : term.definition
                    codeSetTerm.url = !term.url ? "" : term.url
                    codeSet.addToTerms(term)
                }
            }
        }

        def authority = new Authority()
        authority.label = codeSetMap.name
        authority.createdBy = "test@test.com"
        authority.url = codeSetMap.extension.first().url.toString()
        authority.label = codeSetMap.name
        codeSet.label = codeSetMap.name
        codeSet.authority = authority
        codeSet.createdBy = "test@test.com"
        codeSet.label = codeSetMap.name
        processMetadata(codeSetMap, codeSet)

        codeSetService.checkImportedCodeSetAssociations(user, codeSet)
        codeSet
    }

    private void processMetadata(Map<String, Object> codeSetMap, CatalogueItem catalogueItem) {
        codeSetMap.each { key, value ->
            if (!(key in NON_METADATA_KEYS)) {
                catalogueItem.addToMetadata(namespace: namespace, key: key, value: value.toString(), createdBy: "test@test.com")
            }
        }
    }

}
