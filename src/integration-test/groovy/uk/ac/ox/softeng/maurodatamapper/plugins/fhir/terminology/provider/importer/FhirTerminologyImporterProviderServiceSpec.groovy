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

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import com.stehno.ersatz.ErsatzServer
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
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
class FhirTerminologyImporterProviderServiceSpec extends BaseIntegrationSpec {

    FhirTerminologyImporterProviderService fhirTerminologyImporterProviderService
    TerminologyService terminologyService

    @Shared
    ErsatzServer ersatz

    @Shared
    Path resourcesPath

    @OnceBefore
    void setupServerClient() {
        resourcesPath =
            Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'code_systems').toAbsolutePath()
        ersatz = new ErsatzServer()
    }

    void cleanup() {
        ersatz.clearExpectations()
    }

    void cleanupSpec() {
        ersatz.stop()
    }

    @Override
    void setupDomainData() {
        folder = new Folder(label: 'catalogue', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        checkAndSave(folder)
    }

    def 'T01: Test importing administrative-gender'() {
        given:
        setupDomainData()
        String entryId = 'administrative-gender'
        ersatz.expectations {
            GET("/STU3/CodeSystem/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        // Test not passing a version to ensure it defaults to STU3
        def parameters = new FhirTerminologyImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            modelName: entryId
        )

        when:
        Terminology terminology = importAndValidateModel(entryId, parameters)

        then:
        terminology
        terminology.label == entryId
        terminology.aliases.first() == 'AdministrativeGender'
        terminology.metadata.size() == 20

        and:
        terminology.terms.size() == 4
        terminology.terms.collect {it.label}.sort() == ['female: Female', 'male: Male', 'other: Other', 'unknown: Unknown']
        terminology.terms.every {it.description}
    }

    def 'T02: Test importing CareConnect-ConditionCategory-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-ConditionCategory-1'
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/CodeSystem/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirTerminologyImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when:
        Terminology terminology = importAndValidateModel(entryId, parameters)

        then:
        terminology
        terminology.label == entryId
        terminology.aliases.first() == 'Care Connect Condition Category'
        terminology.metadata.size() == 8

        and:
        terminology.terms.size() == 6
        terminology.terms.collect {it.label}.sort() == ['complaint: Complaint', 'diagnosis: Diagnosis', 'finding: Finding', 'need: Need',
                                                        'problem: Problem', 'symptom: Symptom']
    }

    def 'T03: Test importing CareConnect-EthnicCategory-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-EthnicCategory-1'
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/CodeSystem/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirTerminologyImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when:
        Terminology terminology = importAndValidateModel(entryId, parameters)

        then:
        terminology
        terminology.label == entryId
        terminology.aliases.first() == 'Care Connect Ethnic Category'
        terminology.metadata.size() == 9

        and:
        terminology.terms.size() == 68
        terminology.terms.any {it.code == 'A' && it.definition == 'British, Mixed British'}
    }

    def 'T04: Test importing CareConnect-HumanLanguage-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-HumanLanguage-1'
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/CodeSystem/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirTerminologyImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when:
        Terminology terminology = importAndValidateModel(entryId, parameters)

        then:
        terminology
        terminology.label == entryId
        terminology.aliases.first() == 'Care Connect Human Language'
        terminology.metadata.size() == 8

        and:
        terminology.terms.size() == 189
        terminology.terms.any {it.code == 'q5' && it.definition == 'Makaton'}
    }

    private Terminology importAndValidateModel(String entryId, FhirTerminologyImporterProviderServiceParameters parameters) {
        Terminology terminology = fhirTerminologyImporterProviderService.importDomain(admin, parameters)
        assert terminology
        assert terminology.label == entryId
        terminology.folder = folder
        terminologyService.validate(terminology)
        if (terminology.errors.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, terminology)
            throw new ValidationException("Domain object is not valid. Has ${terminology.errors.errorCount} errors", terminology.errors)
        }
        terminology
    }


    String loadJsonString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }
}