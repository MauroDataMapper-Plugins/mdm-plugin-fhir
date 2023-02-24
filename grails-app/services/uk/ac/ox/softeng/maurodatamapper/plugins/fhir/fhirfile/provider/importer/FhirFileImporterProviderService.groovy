/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.fhirfile.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.JsonImportMapping
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.MetadataHandling
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.FhirCodeSetImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.fhirfile.provider.importer.parameter.FhirFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirTerminologyImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

class FhirFileImporterProviderService extends ImporterProviderService<MdmDomain, FhirFileImporterProviderServiceParameters>
    implements MetadataHandling, JsonImportMapping {

    private static List<String> BUNDLE_NON_METADATA_KEYS = ['id', 'entry']

    TerminologyService terminologyService
    FhirTerminologyImporterProviderService fhirTerminologyImporterProviderService
    FhirCodeSetImporterProviderService fhirCodeSetImporterProviderService
    VersionedFolderService versionedFolderService
    CodeSetService codeSetService
    ClassifierService classifierService
    AuthorityService authorityService

    @Override
    String getDisplayName() {
        'FHIR JSON File Importer'
    }

    @Override
    String getNamespace() {
        'uk.ac.ox.softeng.maurodatamapper.plugins.fhir.file'
    }

    @Override
    Boolean handlesContentType(String contentType) {
        contentType.equalsIgnoreCase('application/fhir+json')
    }

    @Override
    Boolean canFederate() {
        true
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    String getProviderType() {
        "FhirFile${ProviderType.IMPORTER.name}"
    }

    @Override
    Boolean allowsExtraMetadataKeys() {
        true
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    MdmDomain importDomain(User user, FhirFileImporterProviderServiceParameters params) {
        if (!user) throw new ApiUnauthorizedException('FHIRFILE01', 'User must be logged in to import model')
        log.debug('Import Model "{}"', params.modelName)

        Map<String, Object> data = slurpAndClean(params.importFile.fileContents, [])

        MdmDomain domain
        if (data.resourceType == 'CodeSystem') {
            // Import FHIR CodeSystem as Mauro Terminology
            domain = importTerminology(user, data, params)
        } else if (data.resourceType == 'ValueSet') {
            // Import FHIR ValueSet as Mauro CodeSet
            domain = importCodeSet(user, data, params)
        } else if (data.resourceType == 'Bundle' && data.type == 'collection') {
            // Import FHIR Bundle as Mauro VersionedFolder
            domain = new VersionedFolder(label: data.id, createdBy: user.emailAddress, authority: params.authority ?: authorityService.getDefaultAuthority())
            processMetadata(data, domain, namespace, BUNDLE_NON_METADATA_KEYS)
            versionedFolderService.checkFacetsAfterImportingMultiFacetAware(domain)
            versionedFolderService.validate(domain)
            versionedFolderService.save(domain)

            data.entry.findAll {it.resource?.resourceType == 'CodeSystem'}.each {Map<String, Object> codeSystemEntry ->
                Terminology terminology = importTerminology(user, codeSystemEntry.resource, params)
                terminology.createdBy = user.emailAddress
                terminology.authority = params.authority ?: authorityService.getDefaultAuthority()
                terminology.folder = domain
                terminologyService.validate(terminology)
                terminologyService.save(terminology)
            }


            data.entry.findAll {it.resource?.resourceType == 'ValueSet'}.each {Map<String, Object> valueSetEntry ->
                CodeSet codeSet = importCodeSet(user, valueSetEntry.resource, params)
                codeSet.createdBy = user.emailAddress
                codeSet.authority = params.authority ?: authorityService.getDefaultAuthority()
                codeSet.folder = domain
                codeSetService.validate(codeSet)
                codeSetService.save(codeSet)
            }
        } else {
            if (!user) throw new ApiUnauthorizedException('FHIRFILE02', 'Cannot import unsupported FHIR Resource Type')
        }

        domain.createdBy = user.emailAddress
        updateImportedDomainFromParameters(domain, params)
        checkDomainImport(user, domain, params)
    }

    @Override
    List<MdmDomain> importDomains(User currentUser, FhirFileImporterProviderServiceParameters params) {
        throw new ApiNotYetImplementedException('FHIRFILE03', 'Importing multiple models not yet implemented')
    }

    Terminology importTerminology(User currentUser, Map<String, Object> data, FhirFileImporterProviderServiceParameters params) {
        Terminology terminology = fhirTerminologyImporterProviderService.extractTerminologyFromData(data)

        terminologyService.checkImportedTerminologyAssociations(currentUser, terminology)
        terminology
    }

    CodeSet importCodeSet(User currentUser, Map<String, Object> data, FhirFileImporterProviderServiceParameters params) {
        CodeSet codeSet = fhirCodeSetImporterProviderService.extractCodeSetFromData(data, currentUser)

        codeSet
    }

    MdmDomain checkDomainImport(User currentUser, MdmDomain importedDomain, FhirFileImporterProviderServiceParameters params) {
        if (importedDomain instanceof Terminology) {
            classifierService.checkClassifiers(currentUser, importedDomain)
            terminologyService.checkAuthority(currentUser, importedDomain, params.useDefaultAuthority)
            terminologyService.checkDocumentationVersion(importedDomain, params.importAsNewDocumentationVersion, currentUser)
            terminologyService.checkBranchModelVersion(importedDomain, params.importAsNewBranchModelVersion, params.newBranchName, currentUser)
            terminologyService.checkFinaliseModel(importedDomain, params.finalised, params.importAsNewBranchModelVersion)
        } else if (importedDomain instanceof CodeSet) {
            classifierService.checkClassifiers(currentUser, importedDomain)
            codeSetService.checkAuthority(currentUser, importedDomain, params.useDefaultAuthority)
            codeSetService.checkDocumentationVersion(importedDomain, params.importAsNewDocumentationVersion, currentUser)
            codeSetService.checkBranchModelVersion(importedDomain, params.importAsNewBranchModelVersion, params.newBranchName, currentUser)
            codeSetService.checkFinaliseModel(importedDomain, params.finalised, params.importAsNewBranchModelVersion)
        } else if (importedDomain instanceof VersionedFolder) {
            versionedFolderService.checkFacetsAfterImportingMultiFacetAware(importedDomain)
            versionedFolderService.checkAuthority(currentUser, importedDomain, params.useDefaultAuthority)
            versionedFolderService.checkDocumentationVersion(importedDomain, params.importAsNewDocumentationVersion, currentUser, params.authority)
            versionedFolderService.checkBranchModelVersion(importedDomain, params.importAsNewBranchModelVersion, params.newBranchName, currentUser,
                                                           params.authority)
            versionedFolderService.checkFinaliseFolder(importedDomain, params.finalised, params.importAsNewBranchModelVersion)
        }
        importedDomain
    }

    MdmDomain updateImportedDomainFromParameters(MdmDomain importedDomain, FhirFileImporterProviderServiceParameters params, boolean list = false) {
        // Dont allow finalisation state to be overridden to unfinalised
        if (params.finalised != null && !importedDomain.finalised) importedDomain.finalised = params.finalised
        if (!list && params.modelName) importedDomain.label = params.modelName
        if (params.author) importedDomain.author = params.author
        if (params.organisation) importedDomain.organisation = params.organisation
        if (params.description) importedDomain.description = params.description
        importedDomain
    }

}
