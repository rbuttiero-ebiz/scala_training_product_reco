package fr.product_reco.service

import fr.product_reco.domain.{ClientId, Order}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MutableMapOrderService extends OrderService {

  private val ordersByClientId: scala.collection.mutable.Map[ClientId, ListBuffer[Order]] = scala.collection.mutable.Map()

  override def addOrder(clientId: ClientId, order: Order): Future[Unit] = Future {
    println(s"Adding order $order")

    val newOrderList = ordersByClientId.getOrElse(clientId, ListBuffer()) += order
    ordersByClientId.put(clientId, newOrderList)
  }

  override def listOrdersForClient(clientId: ClientId): Future[Option[List[Order]]] = Future {
    println(s"Listing all orders for client $clientId : ${ordersByClientId.get(clientId).toList.mkString(", ")}")

    for {
      orders <- ordersByClientId.get(clientId)
    } yield orders.toList
  }
}
