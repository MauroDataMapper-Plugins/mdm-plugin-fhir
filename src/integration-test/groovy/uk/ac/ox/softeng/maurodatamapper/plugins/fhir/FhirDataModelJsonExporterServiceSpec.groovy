package uk.ac.ox.softeng.maurodatamapper.plugins.fhir

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.FhirDataModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.plugins.fhir.datamodel.provider.importer.parameter.FhirDataModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.TestUser

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.file.Path
import java.nio.file.Paths

@Integration
@Rollback
@Slf4j
class FhirDataModelJsonExporterServiceSpec extends BaseFunctionalSpec implements JsonComparer {

    FhirDataModelImporterProviderService fhirDataModelImporterProviderService
    FhirDataModelJsonExporterService fhirDataModelJsonExporterService

    @Shared
    Path resourcesPath

    @OnceBefore
    void setupServerClient() {
        resourcesPath =
            Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'structure_definitions').toAbsolutePath()
    }

    User getAdmin() {
        new TestUser(emailAddress: StandardEmailAddress.ADMIN,
                     firstName: 'Admin',
                     lastName: 'User',
                     organisation: 'Oxford BRC Informatics',
                     jobTitle: 'God',
                     id: UUID.randomUUID())
    }

    def "CC01: verify exported DataModel JSON content - CareConnect-ProcedureRequest-1"() {
        //import FHIR Json file
        given:
        String entryId = 'CareConnect-ProcedureRequest-1'
        def parameters = new FhirDataModelImporterProviderServiceParameters(
            fhirVersion: 'STU3',
            modelName: entryId
        )
        //dataModel = turn Json into dataModel
        when:
        DataModel dataModel = fhirDataModelImporterProviderService.importModel(admin, parameters)
        then:
        dataModel
        dataModel.label == entryId

        //exportJSON = export dataModel into our JSON
        when:
        def exportedJson = (fhirDataModelJsonExporterService.exportDataModel(admin, dataModel))
        then:
        exportedJson
    }

    @Override
    String getResourcePath() {
        ''
    }
}

//dataModel reImportedDataModel = import dataModel(exportJSON)
//dataModelService.diff(exportedDataModel, reImportedDataModel)
//numberOfDiffs == 0