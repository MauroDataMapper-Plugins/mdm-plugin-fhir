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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FhirServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.web.databinding.DataBindingUtils
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired

class FihrTerminologyImporterService extends FhirCodeSystemTerminologyService {

    FhirServerClient serverClient

    @Override
    Terminology importTerminology(User currentUser) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        log.debug("importCodSets")

        def codeSystems = serverClient.getCodeSystemTerminologies('json')
        Map terminology = new JsonSlurper().parseText(codeSystems)
        bindMapToTerminology currentUser, new HashMap(terminology)
    }

    Terminology bindMapToTerminology(User currentUser, Map terminologyMap) {
        if (!terminologyMap) throw new ApiBadRequestException('FBIP03', 'No TerminologyMap supplied to import')

        Terminology terminology = new Terminology()
        terminology.label = terminologyMap.name
        terminology.description = terminologyMap.description
        terminology.version = Long.valueOf(terminologyMap.version.toString().replace('.', '')).longValue()
        log.debug('Binding map to new Terminology instance')
        DataBindingUtils.bindObjectToInstance(terminology, terminologyMap, null, getImportBlacklistedProperties(), null)

        bindTermRelationships(terminology, terminologyMap.concept)

        terminologyService.checkImportedTerminologyAssociations(currentUser, terminology)

        log.info('Import complete')
        terminology
    }

    void bindTermRelationships(Terminology terminology, List<Map> termRelationships) {
        termRelationships.each {tr ->
            Term sourceTerm = new Term()
            sourceTerm.label = tr.code.toString()
            sourceTerm.code = tr.code.toString()
            sourceTerm.definition = tr.definition.toString()
            sourceTerm.version = terminology.version
            terminology.addToTerms(sourceTerm)
/*
            sourceTerm.addToSourceTermRelationships(new TermRelationship(
                    relationshipType: tr.code.toString(),
                    targetTerm: ''
            ))
*/
        }
    }


    @Override
    Boolean canImportMultipleDomains() {
        false
    }
}
