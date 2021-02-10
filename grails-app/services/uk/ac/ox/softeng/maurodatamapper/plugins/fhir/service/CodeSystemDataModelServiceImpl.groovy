package uk.ac.ox.softeng.maurodatamapper.plugins.fhir.service

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.User

class CodeSystemDataModelServiceImpl implements CodeSystemDataModelService{

    @Override
    List<DataModel> importCodeDataModels(User currentUser, def data) {
        if (!currentUser) throw new ApiUnauthorizedException('FHIR0101', 'User must be logged in to import model')
        String namespace = "org.fhir.server"
        List<DataModel> imported = []
        List<DataModel> dataModels = new ArrayList<DataModel>()
        DataModel dataModel = new DataModel(label: data.name)
        dataModel.description = data.description

        dataModels
    }
}
