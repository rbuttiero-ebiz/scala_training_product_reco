package fr.product_reco.service

class ClosestClientRecommendationServiceSpec extends RecommendationServiceSpec {

  override def createRecoService(clientService: ClientService, orderService: OrderService): RecommendationService = new ClosestClientRecommendationService(clientService, orderService)
}
