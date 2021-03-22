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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExportMetadata
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.TemplateBasedExporter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExportModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.plugin.json.view.JsonViewTemplateEngine
import org.springframework.beans.factory.annotation.Autowired

class FhirDataModelJsonExporterService extends DataModelExporterProviderService implements TemplateBasedExporter {

    @Autowired
    JsonViewTemplateEngine templateEngine

    @Override
    String getFileExtension() {
        'json'
    }

    @Override
    String getFileType() {
        'text/json'
    }

    @Override
    String getDisplayName() {
        'JSON FHIR Server DataModel Exporter'
    }

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    String getExportViewPath() {
        '/structureDefinitions/export'
    }

    @Override
    ByteArrayOutputStream exportDataModel(User currentUser, DataModel dataModel) throws ApiException {
        ExportMetadata exportMetadata = new ExportMetadata(this, currentUser.firstName, currentUser.lastName)
        exportModel new DataModelExportModel(dataModel, exportMetadata, false), fileType
    }

    @Override
    ByteArrayOutputStream exportDataModels(User currentUser, List<DataModel> dataModel) throws ApiException {
        throw new ApiBadRequestException('JES01', "${getName()} cannot export multiple DataModels")
    }
}

//sketchings {
//    Map exportModels() {
//        DataModel dataModel
//        dataModelMetadata(dataModel)
//
//        List<Map> dataClasslist
//
//        json.snapshot {
//            json()
//        }
//    }
//
//    private void dataModelMetadata(dataModel) {
//        dataModel.metadata.each {
//            json {
//                it.key it.value
//            }
//        }
//        exportNonMetadata(dataModel)
//    }
//
//    private void exportNonMetadata(dataItem) {
//        json {
//            id dataItem.id
//        }
//        json {
//            description dataItem.description
//        }
//        if (dataItem.minMultiplicity) {
//            json {
//                min dataItem.minMultiplicity
//            }
//        }
//        if (dataItem.maxMultiplicity) {
//            json {
//                //convert back to '*'?
//                max dataItem.maxMultiplicity
//            }
//        }
//    }
//
//    //actual functionality of exporter
//
//    //input dataModel
//
//
//    //for each dataclass of datamodel:
//    //json.snapshot, element {
//    //    key value
//    // note: do not think this will work for alias, constraint, mapping
//    //}
//
//    //json.message {
//    //    hello "world"
//    //} -> {message:{ "hello":"world"}}
//}