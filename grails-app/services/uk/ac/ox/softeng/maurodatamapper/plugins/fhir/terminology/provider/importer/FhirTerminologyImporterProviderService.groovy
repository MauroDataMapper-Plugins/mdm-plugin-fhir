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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.ImportDataHandling
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.MetadataHandling
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.TerminologyImporterProviderService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class FhirTerminologyImporterProviderService extends TerminologyImporterProviderService<FhirTerminologyImporterProviderServiceParameters>
    implements MetadataHandling, ImportDataHandling<Terminology, FhirTerminologyImporterProviderServiceParameters> {

    private static List<String> TERMINOLOGY_NON_METADATA_KEYS = ['id', 'name', 'description', 'publisher', 'concept']
    private static List<String> TERM_NON_METADATA_KEYS = ['code', 'definition', 'display']

    TerminologyService terminologyService

    @Autowired
    ApplicationContext applicationContext

    @Override
    String getDisplayName() {
        'FHIR Server Terminology Importer'
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
    Terminology updateImportedModelFromParameters(Terminology importedModel, FhirTerminologyImporterProviderServiceParameters params, boolean list) {
        updateFhirImportedModelFromParameters(importedModel, params, list)
    }

    @Override
    Terminology checkImport(User currentUser, Terminology importedModel, FhirTerminologyImporterProviderServiceParameters params) {
        checkFhirImport(currentUser, importedModel, params)
    }

    @Override
    Terminology importModel(User user, FhirTerminologyImporterProviderServiceParameters params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        if (!params.modelName) throw new ApiBadRequestException('FHIR02', 'Cannot import a single Terminology without the Terminology name')
        log.debug('Import Terminology {}', params.modelName)
        FhirServerClient fhirServerClient = new FhirServerClient(params.fhirHost, params.fhirVersion, applicationContext)
        importTerminology(fhirServerClient, user, params.fhirVersion, params.modelName)
    }

    @Override
    List<Terminology> importModels(User user, FhirTerminologyImporterProviderServiceParameters params) {
        if (!user) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')

        if (params.modelName) {
            log.debug('Model name supplied, only importing 1 model')
            return [importModel(user, params)]
        }

        log.debug('Import Terminologies version {}', params.fhirVersion ?: 'Current')
        FhirServerClient fhirServerClient = new FhirServerClient(params.fhirHost, params.fhirVersion, applicationContext)
        // Just get the first entry as this will tell us how many there are
        Map<String, Object> countResponse = fhirServerClient.getCodeSystemCount()

        // Now get the full list
        Map<String, Object> valueSets = fhirServerClient.getCodeSystems(countResponse.total as int)

        // Collect all the entries as datamodels
        valueSets.entry.collect {Map entry ->
            importTerminology(fhirServerClient, user, params.fhirVersion, entry.resource.id)
        }
    }

    Terminology importTerminology(FhirServerClient fhirServerClient, User currentUser, String version, String terminologyName) {

        log.debug('Importing Terminology {} from FHIR version {}', terminologyName, version ?: 'Current')

        // Load the map for that datamodel name
        Map<String, Object> data = fhirServerClient.getCodeSystemEntry(terminologyName)

        Terminology terminology = new Terminology(label: data.id, description: data.description, organisation: data.publisher, aliases: [data.name])
        processMetadata(data, terminology, namespace, TERMINOLOGY_NON_METADATA_KEYS)

        data.concept.each {Map concept ->
            Term term = new Term(code: concept.code).tap {
                if (concept.definition) {
                    it.definition = concept.definition
                    it.description = concept.display
                } else {
                    it.definition = concept.display
                }
            }
            processMetadata(concept, term, namespace, TERM_NON_METADATA_KEYS)
            terminology.addToTerms(term)
        }

        terminologyService.checkImportedTerminologyAssociations(currentUser, terminology)
        terminology
    }
}
