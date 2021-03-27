package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer

import groovy.json.JsonSlurper
import org.apache.lucene.util.BytesRef
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.FhirCodeSetService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.parameter.FhirCodeSetImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

class FihrCodeSetImporterService extends FhirCodeSetService {

    public static List<String> NON_METADATA_KEYS = ['concept', "codeSystem"]
    @Autowired
    FHIRServerClient serverClient

    @Autowired
    TerminologyService terminologyService

    @Override
    Boolean canImportMultipleDomains() {
        return false
    }

    @Override
    CodeSet importCodeSet(User currentUser, FhirCodeSetImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR02', 'User must be logged in to import model')
        if (!params.category) throw new ApiUnauthorizedException('FHIR02', 'Category cannot be null')
        if (!params.version) throw new ApiUnauthorizedException('FHIR02', 'Version cannot be null')
        log.debug("importCodSets")
        def category = params.category.toString()
        def version = params.version.toString()

        def codeSets = serverClient.getCodeSets(category, version, 'json')
        Map codeSetMap = new JsonSlurper().parseText(codeSets)
        bindMapToCodeSet currentUser, new HashMap(codeSetMap)
    }

    CodeSet bindMapToCodeSet(User user, HashMap codeSetMap) {
        if (!codeSetMap) throw new ApiBadRequestException('FHIR04', 'No codeSetMap supplied to import')

        def terminologies = terminologyService.findAllByLabel(codeSetMap.name)
        def codeSet = new CodeSet()
        codeSetMap.codeSystem.concept.each { concept ->
            BytesRef bytesRef = new BytesRef()
            terminologies.each { terminology ->
                terminology.terms.each { term ->
                    Term codeSetTerm = new Term()
                    codeSetTerm.label = !term.label ? "" : term.label
                    codeSetTerm.code = !term.code ? "" : term.code
                    codeSetTerm.definition = !term.definition ? "" : term.definition
                    codeSetTerm.url = !term.url ? "" : term.url
                    codeSet.addToTerms(term)
                }
            }
        }
        def authority = new Authority()
        authority.label = codeSetMap.name
        authority.createdBy = "test@test.com"
        authority.url = codeSetMap.extension.first().url.toString()
        authority.label = codeSetMap.name
        codeSet.label = codeSetMap.name
        codeSet.authority = authority
        codeSet.createdBy = "test@test.com"
        codeSet.label = codeSetMap.name
        processMetadata(codeSetMap, codeSet)
        codeSet
    }

    private void  processMetadata(Map<String, Object> codeSetMap, CatalogueItem catalogueItem ) {
        codeSetMap.each { key, value ->
            if (!(key in NON_METADATA_KEYS)) {
                catalogueItem.addToMetadata(namespace: namespace, key: key, value: value.toString(), createdBy: "test@test.com" )
            }
        }
    }

}
