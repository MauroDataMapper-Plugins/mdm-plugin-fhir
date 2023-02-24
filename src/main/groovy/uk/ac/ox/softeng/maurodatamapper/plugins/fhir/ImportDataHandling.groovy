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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.version.Version

import java.time.OffsetDateTime

/**
 * @since 13/04/2021
 */
trait ImportDataHandling<M extends Model, P extends ModelImporterProviderServiceParameters> {

    abstract ClassifierService getClassifierService()

    abstract ModelService<M> getModelService()

    abstract AuthorityService getAuthorityService()

    Authority findOrCreateAuthority(Map data, FhirServerClient fhirServerClient, User currentUser) {
        String label = data.publisher ?: fhirServerClient.getHostUri().host
        if (!label) return null
        Authority authority = authorityService.findByLabel(label)
        if (!authority) {
            authority = new Authority(label: label,
                                      url: fhirServerClient.getHostUri().toString(),
                                      createdBy: currentUser.emailAddress)
            authorityService.save(authority)
        }
        authority
    }

    M updateFhirImportedModelFromParameters(M importedModel, P params, boolean list) {
        if (importedModel.metadata.find {it.key == 'status'}.value in ['retired', 'active']) {
            // Finalise the Mauro model only if the FHIR version is a semantic version, which is recommended but not required
            String metadataVersion = importedModel.metadata.find {it.key == 'version'}.value
            Version finalisedVersion
            try {
                finalisedVersion = Version.from(metadataVersion)
            } catch (Exception ignored) {
            }

            if (finalisedVersion) {
                importedModel.finalised = true

                Metadata finalisedDateMetadata = importedModel.metadata.find {it.key == 'date'} ?:
                                                 importedModel.metadata.find {it.key == 'meta.lastUpdated'}

                String finalisedDate = finalisedDateMetadata.value
                if (finalisedDate.length() < 11) {
                    finalisedDate = finalisedDate + "T00:00:00Z"
                }

                importedModel.dateFinalised = OffsetDateTime.parse(finalisedDate)
                importedModel.modelVersion = finalisedVersion
            } else {
                importedModel.modelVersionTag = metadataVersion
            }
        }
        importedModel
    }

    M checkFhirImport(User currentUser, M importedModel, P params) {
        classifierService.checkClassifiers(currentUser, importedModel)
        modelService.checkAuthority(currentUser, importedModel, params.useDefaultAuthority)
        modelService.checkDocumentationVersion(importedModel, params.importAsNewDocumentationVersion, currentUser)
        modelService.checkBranchModelVersion(importedModel, params.importAsNewBranchModelVersion, params.newBranchName, currentUser)
        importedModel
    }
}