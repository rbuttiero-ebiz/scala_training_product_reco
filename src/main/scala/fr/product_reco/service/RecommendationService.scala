package fr.product_reco.service

import fr.product_reco.domain.{ClientId, ProductId}

import scala.concurrent.Future


trait RecommendationService {

  def computeRecommendation(clientId: ClientId): Future[Option[ProductId]]

}
