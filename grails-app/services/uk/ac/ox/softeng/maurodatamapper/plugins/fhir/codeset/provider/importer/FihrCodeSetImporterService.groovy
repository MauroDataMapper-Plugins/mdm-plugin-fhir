package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer

import groovy.json.JsonSlurper
import org.apache.lucene.util.BytesRef
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.codeset.provider.importer.FhirCodeSetService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.web.client.FHIRServerClient
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet

class FihrCodeSetImporterService extends FhirCodeSetService {

    @Autowired
    FHIRServerClient serverClient

    @Override
    Boolean canImportMultipleDomains() {
        return false
    }

    @Override
    CodeSet importCodeSet(User currentUser) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR01', 'User must be logged in to import model')
        log.debug("importCodSets")

        def codeSets = serverClient.getCodeSets('json')
        Map codeSetMap = new JsonSlurper().parseText(codeSets)
        bindMapToCodeSet currentUser, new HashMap(codeSetMap)
    }

    CodeSet bindMapToCodeSet(User user, HashMap codeSetMap) {
        if (!codeSetMap) throw new ApiBadRequestException('FHIR04', 'No codeSetMap supplied to import')

        def codeSet = new CodeSet()
        codeSetMap.codeSystem.concept.each { concept ->
            BytesRef bytesRef = new BytesRef()
 //           Term term = new Term("man")
 //           codeSet.addToTerms(term)
        }

        codeSet.label = codeSetMap.contained.getAt(0).name
        codeSet.addToMetadata(codeSetMap)

        codeSet
    }
}
