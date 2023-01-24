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

package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.fhirfile.provider

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.fhirfile.provider.importer.FhirFileImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.fhirfile.provider.importer.parameter.FhirFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.util.BuildSettings
import grails.validation.ValidationException
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Integration
@Rollback
class FhirFileImporterProviderServiceSpec extends BaseIntegrationSpec {

    FhirFileImporterProviderService fhirFileImporterProviderService

    @Shared
    Path resourcesPath

    def setupSpec() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'bundles').toAbsolutePath()
    }

    @Override
    void setupDomainData() {
        folder = new Folder(label: 'catalogue', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        checkAndSave(folder)
    }

    void 'FF01 : Test importing codesystem_bundle.json as VersionedFolder'() {
        given:
        setupDomainData()

        FhirFileImporterProviderServiceParameters parameters = new FhirFileImporterProviderServiceParameters(
            importFile: new FileParameter('codesystem_bundle.json', 'json', loadBytes('codesystem_bundle.json')),
            folderId: folder.id
        )

        when:
        VersionedFolder versionedFolder = importAndValidateVersionedFolder('Test-CodeSystem-Bundle', parameters)
        versionedFolder.save(flush: true)

        then:
        Terminology.count() == 2

        when:
        Terminology terminology = Terminology.findByLabel('CodeSystem-1')
        Term term = terminology.findTermByCode('4')

        then:
        terminology.folder == versionedFolder
        terminology.terms.size() == 4
        term.definition == 'A few'

        when:
        terminology = Terminology.findByLabel('CodeSystem-2')
        term = terminology.findTermByCode('L')

        then:
        terminology.folder == versionedFolder
        terminology.terms.size() == 5
        term.definition == 'Linux'
    }

    private byte[] loadBytes(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readAllBytes(testFilePath)
    }

    private VersionedFolder importAndValidateVersionedFolder(String label, FhirFileImporterProviderServiceParameters parameters) {
        VersionedFolder versionedFolder = fhirFileImporterProviderService.importDomain(admin, parameters)
        assert versionedFolder
        assert versionedFolder.label == label
        versionedFolder.parentFolder = folder
        versionedFolder.validate()
        if (versionedFolder.errors.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, versionedFolder)
            throw new ValidationException("Domain object is not valid. Has ${versionedFolder.errors.errorCount} errors", versionedFolder.errors)
        }
        versionedFolder
    }
}
