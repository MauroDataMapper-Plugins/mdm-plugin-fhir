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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir

import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Version

import java.time.OffsetDateTime

/**
 * @since 13/04/2021
 */
trait ImportDataHandling<M extends Model, P extends ModelImporterProviderServiceParameters> {

    abstract ClassifierService getClassifierService()

    abstract ModelService<M> getModelService()

    M updateFhirImportedModelFromParameters(M importedModel, P params, boolean list) {
        if (importedModel.metadata.find {it.key == 'status'}.value == 'active') {
            importedModel.finalised = true

            Metadata finalisedDateMetadata = importedModel.metadata.find {it.key == 'date'} ?:
                                             importedModel.metadata.find {it.key == 'meta.lastUpdated'}

            importedModel.dateFinalised = OffsetDateTime.parse(finalisedDateMetadata.value)
            importedModel.modelVersion = Version.from(importedModel.metadata.find {it.key == 'version'}.value)
        }
        importedModel
    }

    M checkFhirImport(User currentUser, M importedModel, P params) {
        classifierService.checkClassifiers(currentUser, importedModel)
        modelService.checkDocumentationVersion(importedModel, params.importAsNewDocumentationVersion, currentUser)
        modelService.checkBranchModelVersion(importedModel, params.importAsNewBranchModelVersion, params.newBranchName, currentUser)
        importedModel
    }
}