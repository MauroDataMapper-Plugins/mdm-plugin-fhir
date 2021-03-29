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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec

import com.stehno.ersatz.ErsatzServer
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
@Integration
class FhirDataModelImporterProviderServiceSpec extends BaseIntegrationSpec {

    FhirDataModelImporterProviderService fhirDataModelImporterProviderService

    @Shared
    ErsatzServer ersatz

    @Shared
    Path resourcesPath

    @OnceBefore
    void setupServerClient() {
        resourcesPath =
                Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'structure_definitions').toAbsolutePath()
        ersatz = new ErsatzServer()
    }

    void cleanup(){
        ersatz.clearExpectations()
    }

    void cleanupSpec() {
        ersatz.stop()
    }

    @Override
    void setupDomainData() {
    }

    def 'CC01: Test importing CareConnect-ProcedureRequest-1 datamodel'() {
        given:
        String entryId = 'CareConnect-ProcedureRequest-1'
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
            GET("/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(0)
            }
            GET("/StructureDefinition") {
                query('_format', 'json')
                query('_summary','text')
                called(0)
            }
            GET("/StructureDefinition") {
                query('_format', 'json')
                query('_summary','count')
                called(0)
            }
        }
        def parameters = new FhirDataModelImporterProviderServiceParameters(
                fhirHost: ersatz.httpUrl,
                fhirVersion: version,
                modelName: entryId
        )
        when:
        DataModel dataModel = fhirDataModelImporterProviderService.importModel(admin, parameters)
        then:
        dataModel
        dataModel.label == entryId

        dataModel.dataClasses.size() == 13
        dataModel.metadata.size() == 19
        dataModel.childDataClasses.size() == 1

        when:
        DataClass dataClass = dataModel.childDataClasses.first()

        then:
        dataClass
        dataClass.label == 'ProcedureRequest'
        dataClass.minMultiplicity == 0
        dataClass.maxMultiplicity == -1
        dataClass.metadata.size() == 36
        dataClass.dataClasses.size() == 8
        dataClass.dataClasses.collect { it.label } == [
                'identifier', 'requisition', 'category', 'code', 'requester', 'reasonCode', 'bodySite', 'note'
        ]
        dataClass.dataElements.size() == 26
        dataClass.dataElements.collect { it.label } == [
                'id', 'meta', 'implicitRules', 'language', 'text', 'contained', 'extension', 'modifierExtension', /* 'identifier',*/ 'definition',
                'basedOn',
                'replaces', /*'requisition',*/ 'status', 'intent', 'priority', 'doNotPerform', /*'category', 'code', */ 'subject', 'context',
                'occurrence[x]', 'asNeeded[x]', 'authoredOn',
                /*'requester', */ 'performerType', 'performer', /*'reasonCode', */ 'reasonReference', 'supportingInfo', 'specimen', /*'bodySite',
            'note',*/
                'relevantHistory',
        ]

        when:
        DataClass subDataClass = dataClass.dataClasses.find { it.label == 'identifier' }

        then:
        !subDataClass.dataClasses
        subDataClass.dataElements.size() == 8
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'use', 'type', 'system', 'value', 'period', 'assigner'
        ]

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'requisition' }

        then:
        !subDataClass.dataClasses
        subDataClass.dataElements.size() == 8
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'use', 'type', 'system', 'value', 'period', 'assigner'
        ]

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'category' }

        then:
        subDataClass.dataElements.size() == 4
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'coding', /*'coding:snomedCT',*/ 'text'
        ]
        subDataClass.dataClasses.size() == 1
        subDataClass.dataClasses.first().label == 'coding:snomedCT'
        subDataClass.dataClasses.first().dataElements.size() == 8
        subDataClass.dataClasses.first().dataElements.collect { it.label } == [
                'id', 'extension', 'extension:snomedCTDescriptionID', 'system', 'version', 'code', 'display', 'userSelected'
        ]

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'code' }

        then:
        subDataClass.dataElements.size() == 4
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'coding',/* 'coding:snomedCT', */ 'text'
        ]
        subDataClass.dataClasses.size() == 1
        subDataClass.dataClasses.first().label == 'coding:snomedCT'
        subDataClass.dataClasses.first().dataElements.size() == 8
        subDataClass.dataClasses.first().dataElements.collect { it.label } == [
                'id', 'extension', 'extension:snomedCTDescriptionID', 'system', 'version', 'code', 'display', 'userSelected'
        ]

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'requester' }

        then:
        !subDataClass.dataClasses
        subDataClass.dataElements.size() == 5
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'modifierExtension', 'agent', 'onBehalfOf'
        ]

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'reasonCode' }

        then:
        subDataClass.dataElements.size() == 4
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'coding', /*'coding:snomedCT', */ 'text'
        ]
        subDataClass.dataClasses.size() == 1
        subDataClass.dataClasses.first().label == 'coding:snomedCT'
        subDataClass.dataClasses.first().dataElements.size() == 8
        subDataClass.dataClasses.first().dataElements.collect { it.label } == [
                'id', 'extension', 'extension:snomedCTDescriptionID', 'system', 'version', 'code', 'display', 'userSelected'
        ]

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'bodySite' }

        then:
        subDataClass.dataElements.size() == 4
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'coding', /*'coding:snomedCT',*/ 'text'
        ]
        subDataClass.dataClasses.size() == 1
        subDataClass.dataClasses.first().label == 'coding:snomedCT'
        subDataClass.dataClasses.first().dataElements.size() == 8
        subDataClass.dataClasses.first().dataElements.collect { it.label } == [
                'id', 'extension', 'extension:snomedCTDescriptionID', 'system', 'version', 'code', 'display', 'userSelected'
        ]

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'note' }

        then:
        !subDataClass.dataClasses
        subDataClass.dataElements.size() == 5
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'author[x]', 'time', 'text'
        ]
    }

    def 'CC02: Test importing CareConnect-OxygenSaturation-Observation-1 datamodel'() {
        given:
        String entryId = 'CareConnect-OxygenSaturation-Observation-1'
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
            GET("/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(0)
            }
        }
        def parameters = new FhirDataModelImporterProviderServiceParameters(
                fhirHost: ersatz.httpUrl,
                fhirVersion: version,
                modelName: entryId
        )
        when:
        DataModel dataModel = fhirDataModelImporterProviderService.importModel(admin, parameters)

        then:
        dataModel
        dataModel.label == entryId
        dataModel.dataClasses.size() == 23
        dataModel.metadata.size() == 14
        dataModel.childDataClasses.size() == 1

        when:
        DataClass dataClass = dataModel.childDataClasses.first()

        then:
        dataClass
        dataClass.label == 'Observation'
        dataClass.minMultiplicity == 0
        dataClass.maxMultiplicity == -1
        dataClass.metadata.size() == 52
        dataClass.dataClasses.size() == 10
        dataClass.dataClasses.collect { it.label } == [
                'identifier', 'category', 'code', 'valueQuantity', 'dataAbsentReason', 'bodySite', 'method', 'referenceRange', 'related', 'component'
        ]
        dataClass.dataElements.size() == 19
        dataClass.dataElements.collect { it.label } == [
                'id', 'meta', 'implicitRules', 'language', 'text', 'contained', 'extension', 'modifierExtension',/* 'identifier', */ 'basedOn', 'status',
                /*'category', 'code',*/ 'subject', 'context', 'effective[x]', 'issued', 'performer', /*'valueQuantity', 'dataAbsentReason',*/
                'interpretation',
                'comment',
                /*'bodySite', 'method',*/ 'specimen', 'device', /*'referenceRange', 'related', 'component',*/
        ]

        when:
        DataClass subDataClass = dataClass.dataClasses.find { it.label == 'category' }

        then:
        subDataClass
        subDataClass.dataClasses.size() == 1
        subDataClass.dataClasses.first().label == 'coding'

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'code' }

        then:
        subDataClass
        subDataClass.dataClasses.size() == 2
        subDataClass.dataClasses.any { it.label == 'coding:snomedCT' }
        subDataClass.dataClasses.any { it.label == 'coding:loinc' }

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'valueQuantity' }

        then:
        subDataClass
        !subDataClass.dataClasses
        subDataClass.dataElements.size() == 7
        subDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'value', 'comparator', 'unit', 'system', 'code'
        ]

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'bodySite' }

        then:
        subDataClass
        subDataClass.dataClasses.size() == 1
        subDataClass.dataClasses.first().label == 'coding:snomedCT'

        when:
        subDataClass = dataClass.dataClasses.find { it.label == 'method' }

        then:
        subDataClass
        subDataClass.dataClasses.size() == 1
        subDataClass.dataClasses.first().label == 'coding:snomedCT'

        when:
        DataClass componentDataClass = dataClass.dataClasses.find { it.label == 'component' }

        then:
        componentDataClass
        componentDataClass.dataClasses.size() == 3
        componentDataClass.dataClasses.any { it.label == 'code' }
        componentDataClass.dataClasses.any { it.label == 'valueQuantity' }
        componentDataClass.dataClasses.any { it.label == 'dataAbsentReason' }


        when:
        DataClass codeComponentDataClass = componentDataClass.dataClasses.find { it.label == 'code' }

        then:
        codeComponentDataClass
        codeComponentDataClass.dataClasses.size() == 2
        codeComponentDataClass.dataClasses.any { it.label == 'coding:snomedCT' }
        codeComponentDataClass.dataClasses.any { it.label == 'coding:loinc' }

        when:
        DataClass valueQuantityComponentDataClass = componentDataClass.dataClasses.find { it.label == 'valueQuantity' }

        then:
        valueQuantityComponentDataClass
        !valueQuantityComponentDataClass.dataClasses
        valueQuantityComponentDataClass.dataElements.size() == 7
        valueQuantityComponentDataClass.dataElements.collect { it.label } == [
                'id', 'extension', 'value', 'comparator', 'unit', 'system', 'code'
        ]

        when:
        DataClass dataAbsentComponentDataClass = componentDataClass.dataClasses.find {it.label == 'dataAbsentReason'}

        then:
        dataAbsentComponentDataClass
        dataAbsentComponentDataClass.dataClasses.size() == 1
        dataAbsentComponentDataClass.dataClasses.first().label == 'coding'
    }

    def 'CC03: Test importing CareConnect-GPC-MedicationRequest-1'() {
        // This datamodel has "orphaned" dataelements
        given:
        String entryId = 'CareConnect-GPC-MedicationRequest-1'
        String version = 'STU3'
        ersatz.expectations {
            GET("/$version/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("${entryId}.json"))
                }
            }
            GET("/StructureDefinition/$entryId") {
                query('_format', 'json')
                called(0)
            }
        }
        def parameters = new FhirDataModelImporterProviderServiceParameters(
                fhirHost: ersatz.httpUrl,
                fhirVersion: version,
                modelName: entryId
        )

        when:
        DataModel dataModel = fhirDataModelImporterProviderService.importModel(admin, parameters)

        then:
        noExceptionThrown()
        dataModel
    }

    def 'Test importing current multiple datamodels'() {
        given:
        ersatz.expectations {
            GET("/StructureDefinition") {
                query('_format', 'json')
                query('_summary', 'text')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString('StructureDefintion_text.json'))
                }
            }
            GET("/StructureDefinition") {
                query('_format', 'json')
                query('_summary','count')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString('StructureDefintion_count.json'))
                }
            }
            GET("/StructureDefinition/CareConnect-Condition-1") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("CareConnect-Condition-1.json"))
                }
            }
            GET("/StructureDefinition/Extension-CareConnect-AdmissionMethod-1") {
                query('_format', 'json')
                called(1)
                responder {
                    contentType('application/json')
                    code(200)
                    body(loadJsonString("Extension-CareConnect-AdmissionMethod-1.json"))
                }
            }
            GET("/STU3/StructureDefinition/CareConnect-Condition-1") {
                query('_format', 'json')
                called(0)
            }
            GET("/STU3/StructureDefinition/Extension-CareConnect-AdmissionMethod-1") {
                query('_format', 'json')
                called(0)
            }
        }
        def parameters = new FhirDataModelImporterProviderServiceParameters(
                fhirHost: ersatz.httpUrl,
        )
        when:
        List<DataModel> imported = fhirDataModelImporterProviderService.importModels(admin, parameters)

        then:
        // Current has 51 models but we only test 2 for speed and memory
        imported.size() == 2
        imported.any { it.label == 'CareConnect-Condition-1' }
        imported.any { it.label == 'Extension-CareConnect-AdmissionMethod-1' }
    }

    Map loadJsonMap(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        String content = Files.readString(testFilePath)
        new JsonSlurper().parseText(content) as Map
    }

    String loadJsonString(String filename) {
        Path testFilePath = resourcesPath.resolve("${filename}").toAbsolutePath()
        assert Files.exists(testFilePath)
        Files.readString(testFilePath)
    }
}