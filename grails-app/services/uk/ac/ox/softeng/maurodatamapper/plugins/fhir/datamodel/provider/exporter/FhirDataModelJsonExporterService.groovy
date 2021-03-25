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
package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.TemplateBasedExporter
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.exporter.DataModelExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.plugin.json.view.JsonViewTemplateEngine
import groovy.text.Template
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
        '/structureDefinition/export'
    }

    @Override
    ByteArrayOutputStream exportDataModel(User currentUser, DataModel dataModel) throws ApiException {
        exportModel(dataModel, fileType)
    }

    ByteArrayOutputStream exportModel(DataModel dataModel, String format) {
        Template template = templateEngine.resolveTemplate(exportViewPath)

        if (!template) {
            log.error('Could not find template for format {} at path {}', format, exportViewPath)
            throw new ApiInternalException('TBE02', "Could not find template for format ${format} at path ${exportViewPath}")
        }

        def writable = template.make(dataModel: dataModel)
        def sw = new StringWriter()
        writable.writeTo(sw)
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        os.write(sw.toString().bytes)
        os
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
