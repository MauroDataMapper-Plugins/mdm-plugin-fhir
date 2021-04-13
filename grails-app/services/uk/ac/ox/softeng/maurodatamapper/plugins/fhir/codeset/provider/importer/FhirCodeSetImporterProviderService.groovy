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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentation
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.MetadataHandling
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter.FhirCodeSetImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.CodeSetImporterProviderService

import io.micronaut.http.uri.UriBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import java.time.OffsetDateTime

class FhirCodeSetImporterProviderService extends CodeSetImporterProviderService<FhirCodeSetImporterProviderServiceParameters>
    implements MetadataHandling {

    private static List<String> NON_METADATA_KEYS = ['contains', 'id', 'name', 'description', 'publisher', 'codeSystem', 'compose']

    TerminologyService terminologyService

    @Autowired
    ApplicationContext applicationContext

    @Override
    String getDisplayName() {
        'FHIR Server CodeSet Importer'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    Boolean allowsExtraMetadataKeys() {
        true
    }

    @Override
    Boolean canImportMultipleDomains() {
        true
    }

    @Override
    CodeSet updateImportedModelFromParameters(CodeSet importedModel, FhirCodeSetImporterProviderServiceParameters params, boolean list) {
        if (importedModel.metadata.find {it.key == 'status'}.value == 'active') {
            importedModel.finalised = true
            importedModel.dateFinalised = importedModel.metadata.find {it.key == 'date'}.value as OffsetDateTime
            importedModel.version = importedModel.metadata.find {it.key == 'version'}.value as Long
        }
        importedModel
    }

    @Override
    CodeSet checkImport(User currentUser, CodeSet importedModel, FhirCodeSetImporterProviderServiceParameters params) {
        classifierService.checkClassifiers(currentUser, importedModel)
        modelService.checkDocumentationVersion(importedModel, params.importAsNewDocumentationVersion, currentUser)
        modelService.checkBranchModelVersion(importedModel, params.importAsNewBranchModelVersion, params.newBranchName, currentUser)
        importedModel
    }

    @Override
    CodeSet importModel(User user, FhirCodeSetImporterProviderServiceParameters params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        if (!params.modelName) throw new ApiBadRequestException('FHIR02', 'Cannot import a single CodeSet without the CodeSet name')
        log.debug('Import CodeSet {}', params.modelName)
        FhirServerClient fhirServerClient = new FhirServerClient(params.fhirHost, params.fhirVersion, applicationContext)
        importCodeSet(fhirServerClient, user, params.fhirVersion, params.modelName)
    }

    @Override
    List<CodeSet> importModels(User user, FhirCodeSetImporterProviderServiceParameters params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')

        if (params.modelName) {
            log.debug('Model name supplied, only importing 1 model')
            return [importModel(user, params)]
        }

        log.debug('Import CodeSets version {}', params.fhirVersion ?: 'Current')
        FhirServerClient fhirServerClient = new FhirServerClient(params.fhirHost, params.fhirVersion, applicationContext)
        // Just get the first entry as this will tell us how many there are
        Map<String, Object> countResponse = fhirServerClient.getValueSetCount()

        // Now get the full list
        Map<String, Object> valueSets = fhirServerClient.getValueSets(countResponse.total as int)

        // Collect all the entries as datamodels
        valueSets.entry.collect {Map entry ->
            importCodeSet(fhirServerClient, user, params.fhirVersion, entry.resource.id)
        }
    }

    CodeSet importCodeSet(FhirServerClient fhirServerClient, User currentUser, String version, String codeSetName) {

        log.debug('Importing CodeSet {} from FHIR version {}', codeSetName, version ?: 'Current')

        // Load the map for that datamodel name
        Map<String, Object> data = fhirServerClient.getValueSetEntry(codeSetName)


        CodeSet codeSet = new CodeSet(label: data.id, description: data.description, organisation: data.publisher, aliases: [data.name])
        processMetadata(data, codeSet, namespace, NON_METADATA_KEYS)

        // TODO provide addtl param to import terminology if its not found
        if (data.codeSystem) {
            // Older format where the codeSystem/terminology seems to be included inside the valueset,
            // Its unlikely we will find a relevant endpoint to import the terminology for this codesystem
            loadTermsFromCodeSystemMap(codeSet, data.codeSystem as Map<String, Object>)
        } else if (data.compose) {
            // STU3 format (and newer) requires the codeSystem to be imported separately
            loadTermsFromComposeMap(codeSet, data.compose as Map<String, Object>)
        } else {
            throw new ApiInternalException('FCCXX', 'Unknown Value Set "extension" type')
        }

        codeSetService.checkImportedCodeSetAssociations(currentUser, codeSet)
        //Temp hack until the check facets also updated rules
        if (codeSet.rules) {
            codeSet.rules.each {r ->
                r.createdBy = codeSet.createdBy
                r.ruleRepresentations.each {rr ->
                    rr.createdBy = codeSet.createdBy
                }
            }
        }
        codeSet
    }

    private void loadTermsFromCodeSystemMap(CodeSet codeSet, Map<String, Object> codeSystemMap) {
        URI systemUrl = UriBuilder.of(codeSystemMap.system as String).build()
        String terminologyLabel = systemUrl.path.split('/').last()
        Terminology terminology = terminologyService.findLatestModelByLabel(terminologyLabel)

        if (!terminology) {
            throw new ApiBadRequestException('FCC01', "Cannot import CodeSet without importing [${terminologyLabel}] first")
        }

        codeSystemMap.concept.each {concept ->
            Term term = terminology.findTermByCode(concept.code)
            if (!term) {
                throw new ApiBadRequestException('FCC02', "Term [${concept.code}] does not exist inside terminology [${terminologyLabel}]")
            }
            codeSet.addToTerms(term)
        }
    }

    private void loadTermsFromComposeMap(CodeSet codeSet, Map<String, Object> composeMap) {
        composeMap.include.each {include ->
            loadTermsFromIncludeMap(codeSet, include as Map<String, Object>)
        }
    }

    private void loadTermsFromIncludeMap(CodeSet codeSet, Map<String, Object> includeMap) {
        String terminologyLabel
        // Special handling for SNOMED
        if (includeMap.system == 'http://snomed.info/sct') {
            terminologyLabel = 'SNOMED CT January 2018 International Edition'
        } else {
            URI systemUrl = UriBuilder.of(includeMap.system as CharSequence).build()
            terminologyLabel = systemUrl.path.split('/').last()
        }

        Terminology terminology = terminologyService.findLatestModelByLabel(terminologyLabel)

        if (!terminology) {
            throw new ApiBadRequestException('FCC01', "Cannot import CodeSet without importing [${terminologyLabel}] first")
        }

        // We will need to handle processing this filter at some point possibly
        if (includeMap.filter) {
            Rule rule = new Rule(name: "${terminologyLabel} filter",
                                 description: 'Filter to be applied to the defined terminology to obtain the terms')
            includeMap.filter.each {filter ->
                rule.addToRuleRepresentations(new RuleRepresentation(language: "FHIR ${filter.property} ${filter.op}",
                                                                     representation: filter.value
                ))
            }
            codeSet.addToRules(rule)
        } else {
            codeSet.terms = new HashSet<>(terminology.terms)
        }
    }
}
