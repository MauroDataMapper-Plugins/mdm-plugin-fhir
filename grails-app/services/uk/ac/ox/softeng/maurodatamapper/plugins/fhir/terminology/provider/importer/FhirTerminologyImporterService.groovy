package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer

import grails.web.databinding.DataBindingUtils
import groovy.json.JsonSlurper
import org.apache.commons.text.StringEscapeUtils
import org.hsqldb.lib.StringUtil
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.FhirCodeSystemTerminologyService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.terminology.provider.importer.parameter.FhirTerminologyImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.util.Version

class FhirTerminologyImporterService extends FhirCodeSystemTerminologyService {

    public static List<String> NON_METADATA_KEYS = ['concept', "codeSystem"]

    @Autowired
    FHIRServerClient serverClient

  //  @Override
    Terminology importTerminology(User currentUser, FhirTerminologyImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        if (!params.category) throw new ApiUnauthorizedException('FHIR01', 'Category cannot be null')
        if (!params.version) throw new ApiUnauthorizedException('FHIR01', 'Version cannot be null')
        log.debug("Terminology")
        def category = params.category.toString()
        def version = params.version.toString()
        def codeSystems = serverClient.getCodeSystemTerminologies(category, version, 'json')
        Map terminology = new JsonSlurper().parseText(codeSystems)
        bindMapToTerminology (currentUser, new HashMap(terminology))
    }

    Terminology bindMapToTerminology(User currentUser, Map terminologyMap) {
        if (!terminologyMap) throw new ApiBadRequestException('FBIP03', 'No TerminologyMap supplied to import')
        terminologyMap.remove("version")
        Terminology terminology = new Terminology()
        terminology.label = terminologyMap.name
        terminology.description = terminologyMap.description
        log.debug('Binding map to new Terminology instance')
        DataBindingUtils.bindObjectToInstance(terminology, terminologyMap, null, getImportBlacklistedProperties(), null)

        bindTermRelationships(terminology, terminologyMap.concept)

        processMetadata(terminologyMap, terminology)
        terminologyService.checkImportedTerminologyAssociations(currentUser, terminology)

        log.info('Import complete')
        terminology
    }

    public void bindTermRelationships(Terminology terminology, List<Map> termRelationships) {
        termRelationships.each {tr ->
            Term sourceTerm = new Term()
            sourceTerm.label = tr.code.toString()
            sourceTerm.code = tr.code.toString()
            sourceTerm.definition = tr.definition.toString()
            terminology.addToTerms(sourceTerm)
        }
    }

    private void  processMetadata(Map<String, Object> codeSetMap, CatalogueItem catalogueItem ) {
        codeSetMap.each { key, value ->
            if (!(key in NON_METADATA_KEYS)) {
                catalogueItem.addToMetadata(namespace: namespace, key: key, value: value.toString())
            }
        }
    }


    @Override
    Boolean canImportMultipleDomains() {
        false
    }
}
