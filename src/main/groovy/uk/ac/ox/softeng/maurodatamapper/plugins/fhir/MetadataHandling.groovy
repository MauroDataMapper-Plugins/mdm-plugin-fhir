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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

/**
 * @since 29/03/2021
 */
trait MetadataHandling {

    void processMetadata(Map<String, Object> dataset, CatalogueItem dataItem, String namespace, List<String> nonMetadataKeys) {
        dataset.each {key, value ->
            if (!(key in nonMetadataKeys)) {
                if (!(value instanceof String || value instanceof Integer || value instanceof Boolean)) {
                    processNestedMetadata(key, value, dataItem, namespace)
                } else {
                    dataItem.addToMetadata(
                        namespace: namespace,
                        key: key,
                        value: value.toString()
                    )
                }
            }
        }
    }

    void processNestedMetadata(String key, dataCollection, CatalogueItem dataItem, String namespace) {
        try {
            if (dataCollection instanceof List) {
                processListedMetadata(key, dataCollection, dataItem, namespace)
            }
            if (dataCollection instanceof Map) {
                processMappedMetadata(key, dataCollection, dataItem, namespace)
            }
        } catch (Exception ex) {
            throw new ApiInternalException('FHIR04', 'bad nesting, nests are either map or list', ex)
        }
    }

    void processListedMetadata(String key, List list, CatalogueItem dataItem, String namespace) {
        if (list.size() > 1) {
            list.eachWithIndex {item, index ->
                if (item instanceof String || item instanceof Integer || item instanceof Boolean) {
                    dataItem.addToMetadata(
                        namespace: namespace,
                        key: "$key[${index}]",
                        value: item.toString()
                    )
                } else if (item instanceof Map) {
                    processMappedMetadata("$key[${index}]", item, dataItem, namespace)
                }
            }
        } else {
            list.each {item ->
                if (item instanceof String || item instanceof Integer || item instanceof Boolean) {
                    dataItem.addToMetadata(
                        namespace: namespace,
                        key: key,
                        value: item.toString()
                    )
                } else if (item instanceof Map) {
                    processMappedMetadata(key, item, dataItem, namespace)
                }
            }
        }
    }

    void processMappedMetadata(String key, Map map, CatalogueItem dataItem, String namespace) {
        map.each {mapKey, mapVal ->
            if (mapVal instanceof String || mapVal instanceof Integer || mapVal instanceof Boolean) {
                dataItem.addToMetadata(
                    namespace: namespace,
                    key: "${key}.${mapKey}",
                    value: mapVal.toString()
                )
            }
            if (mapVal instanceof List) {
                processListedMetadata("${key}.${mapKey}", mapVal, dataItem, namespace)
            }
        }
    }


}
