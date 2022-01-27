/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter.FhirCodeSetImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirTerminologyImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import com.stehno.ersatz.ErsatzServer
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.testing.spock.RunOnce
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
class FhirCodeSetImporterProviderServiceSpec extends BaseIntegrationSpec {

    FhirCodeSetImporterProviderService fhirCodeSetImporterProviderService
    FhirTerminologyImporterProviderService fhirTerminologyImporterProviderService
    CodeSetService codeSetService
    AuthorityService authorityService
    TerminologyService terminologyService

    @Shared
    ErsatzServer ersatz

    @Shared
    Path resourcesPath
    @Shared
    Path codeSystemsResourcesPath

    def setupSpec() {
        resourcesPath =
            Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'value_sets').toAbsolutePath()
        codeSystemsResourcesPath =
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
        // Do not need to actually import SNOMED. We need to create an empty terminology as we're creating filter rules
        Terminology snomedCt = new Terminology(label: 'SNOMED CT January 2018 International Edition',
                                               createdBy: StandardEmailAddress.INTEGRATION_TEST,
                                               authority: authorityService.getDefaultAuthority(),
                                               folder: folder)

        checkAndSave(snomedCt)
    }


    def 'CS01: Test importing CareConnect-AdministrativeGender-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-AdministrativeGender-1'
        importTerminology('administrative-gender')
        ersatz.expectations {
            GET("/ValueSet/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirCodeSetImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            modelName: entryId
        )

        when:
        CodeSet codeSet = importAndValidateModel(entryId, parameters)

        then:
        codeSet
        codeSet.metadata.size() == 28
        codeSet.terms.size() == 4
    }

    def 'CS02: Test importing CareConnect-AllergyCertainty-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-AllergyCertainty-1'
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/ValueSet/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirCodeSetImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when:
        CodeSet codeSet = importAndValidateModel(entryId, parameters)

        then:
        codeSet
        codeSet.metadata.size() == 6
        !codeSet.terms
        codeSet.rules.size() == 1
        codeSet.rules.first().name == 'SNOMED CT January 2018 International Edition filter'
        codeSet.rules.first().ruleRepresentations.size() == 1
        codeSet.rules.first().ruleRepresentations.first().language == 'fhir constraint ='
        codeSet.rules.first().ruleRepresentations.first().representation ==
        '(385434005 |Improbable diagnosis| OR 2931005 |Probable diagnosis| OR 255545003 |Definite| OR 410605003 |Confirmed present|)'
    }

    def 'CS03: Test importing CareConnect-ConditionCategory-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-ConditionCategory-1'
        importTerminology(entryId)
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/ValueSet/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirCodeSetImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when:
        CodeSet codeSet = importAndValidateModel(entryId, parameters)

        then:
        codeSet
        codeSet.metadata.size() == 6
        codeSet.terms.size() == 6
    }

    def 'CS04: Test importing CareConnect-EthnicCategory-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-EthnicCategory-1'
        importTerminology(entryId)
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/ValueSet/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirCodeSetImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            fhirVersion: version,
            modelName: entryId
        )

        when:
        CodeSet codeSet = importAndValidateModel(entryId, parameters)

        then:
        codeSet
        codeSet.metadata.size() == 10
        codeSet.terms.size() == 68
    }

    def 'CS05: Test importing CareConnect-PersonStatedGender-DDMAP-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-PersonStatedGender-DDMAP-1'
        ersatz.expectations {
            GET("/ValueSet/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirCodeSetImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            modelName: entryId
        )

        when:
        importAndValidateModel(entryId, parameters)

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'FCC01'
        exception.message == "Cannot import CodeSet without importing [${entryId}] first"
    }

    def 'CS06: Test importing CareConnect-HumanLanguage-1'() {
        given:
        setupDomainData()
        String entryId = 'CareConnect-HumanLanguage-1'
        importTerminology(entryId)
        ersatz.expectations {
            GET("/ValueSet/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
        }
        def parameters = new FhirCodeSetImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            modelName: entryId
        )

        when:
        importAndValidateModel(entryId, parameters)

        then: 'As the terminology has been loaded from STU3 and the CodeSet from the root the concepts arent the same'
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.errorCode == 'FCC02'
        exception.message == 'Term [mo] does not exist inside terminology [CareConnect-HumanLanguage-1]'
    }

    private CodeSet importAndValidateModel(String entryId, FhirCodeSetImporterProviderServiceParameters parameters) {
        CodeSet codeSet = fhirCodeSetImporterProviderService.importDomain(admin, parameters)
        assert codeSet
        assert codeSet.label == entryId
        codeSet.folder = folder
        codeSetService.validate(codeSet)
        if (codeSet.errors.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, codeSet)
            throw new ValidationException("Domain object is not valid. Has ${codeSet.errors.errorCount} errors", codeSet.errors)
        }
        codeSet
    }


    String loadJsonString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }

    String loadTerminologyJsonString(String filename) {
        Path testFilePath = codeSystemsResourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }

    private void importTerminology(String terminologyId) {
        ersatz.expectations {
            GET("/STU3/CodeSystem/$terminologyId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadTerminologyJsonString("${terminologyId}.json"))
                }
            }
        }
        def parameters = new FhirTerminologyImporterProviderServiceParameters(
            fhirHost: ersatz.httpUrl,
            modelName: terminologyId
        )
        Terminology terminology = fhirTerminologyImporterProviderService.importDomain(admin, parameters)
        terminology.folder = folder
        terminologyService.validate(terminology)
        if (terminology.errors.hasErrors()) {
            GormUtils.outputDomainErrors(messageSource, terminology)
            throw new ValidationException("Domain object is not valid. Has ${terminology.errors.errorCount} errors", terminology.errors)
        }
        terminologyService.saveModelWithContent(terminology)
    }
}