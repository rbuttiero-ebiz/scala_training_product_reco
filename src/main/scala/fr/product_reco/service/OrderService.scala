package fr.product_reco.service

import fr.product_reco.domain.ClientId
import fr.product_reco.domain.Order

import scala.concurrent.Future

trait OrderService {

  def addOrder(clientId: ClientId, order: Order): Future[Unit]

  def listOrdersForClient(clientId: ClientId): Future[Option[List[Order]]]

}
