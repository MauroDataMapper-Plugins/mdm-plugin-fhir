package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.service


import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.User

interface CodeSystemDataModelService {

    List<DataModel> importCodeDataModels(User currentUser, def data)
}
