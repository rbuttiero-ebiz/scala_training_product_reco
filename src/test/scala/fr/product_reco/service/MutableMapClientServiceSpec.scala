package fr.product_reco.service

class MutableMapClientServiceSpec extends ClientServiceSpec {

  override def createClientService: ClientService = new MutableMapClientService()
}
