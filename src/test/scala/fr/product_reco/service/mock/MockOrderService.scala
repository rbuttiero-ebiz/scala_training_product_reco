package fr.product_reco.service.mock

import java.time.LocalDateTime

import fr.product_reco.domain.{ClientId, Order, OrderId, OrderItem, OrderItemId, Product, ProductId}
import fr.product_reco.service.OrderService

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class MockOrderService extends OrderService {

  val order1_1 = Order(OrderId(1), LocalDateTime.now(), List(
    OrderItem(OrderItemId(11), Product(ProductId(1)), 1),
    OrderItem(OrderItemId(12), Product(ProductId(2)), 2)
  ))
  val order1_2 = Order(OrderId(2), LocalDateTime.now(), List(
    OrderItem(OrderItemId(21), Product(ProductId(2)), 1),
    OrderItem(OrderItemId(22), Product(ProductId(3)), 2)
  ))
  val order1_3 = Order(OrderId(2), LocalDateTime.now(), List(
    OrderItem(OrderItemId(21), Product(ProductId(4)), 1)
  ))
  val order2_1 = Order(OrderId(1), LocalDateTime.now(), List(
    OrderItem(OrderItemId(11), Product(ProductId(1)), 1),
    OrderItem(OrderItemId(12), Product(ProductId(2)), 1)
  ))

  override def addOrder(clientId: ClientId, order: Order): Future[Unit] = Future.successful()

  override def listOrdersForClient(clientId: ClientId): Future[Option[List[Order]]] = Future {
    clientId match {
      case ClientId(1) => Some(List(order1_1, order1_2, order1_3))
      case ClientId(2) => Some(List(order2_1))
      case ClientId(3) => None
      case _ => None
    }
  }
}
