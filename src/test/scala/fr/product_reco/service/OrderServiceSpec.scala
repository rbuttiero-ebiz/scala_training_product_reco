package fr.product_reco.service

import java.time.LocalDateTime

import fr.product_reco.domain._
import org.scalatest.{Matchers, _}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

abstract class OrderServiceSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterEach with Inside {

  val order1 = Order(OrderId(1), LocalDateTime.now(), List(
    OrderItem(OrderItemId(11), Product(ProductId(1)), 1),
    OrderItem(OrderItemId(12), Product(ProductId(2)), 2)
  ))
  val order2 = Order(OrderId(2), LocalDateTime.now(), List(
    OrderItem(OrderItemId(21), Product(ProductId(1)), 1),
    OrderItem(OrderItemId(22), Product(ProductId(2)), 2)
  ))
  var orderService: OrderService = _

  def createOrderService: OrderService

  override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def beforeEach() {
    orderService = createOrderService
    Await.ready(orderService.addOrder(ClientId(1), order1), Duration.Inf)
  }

  "Order Service" should "find orders for existing client" in {
    for {
      orders <- orderService.listOrdersForClient(ClientId(1))
    } yield inside(orders) {
      case Some(List(o1)) =>
        o1 shouldBe order1
    }
  }

  "Order Service" should "not find orders for a missing client" in {
    for {
      client <- orderService.listOrdersForClient(ClientId(99))
    } yield client shouldBe None
  }

  "Order Service" should "add order for a new client" in {
    for {
      _ <- orderService.addOrder(ClientId(2), order2)
      orders <- orderService.listOrdersForClient(ClientId(2))
    } yield inside(orders) {
      case Some(List(o2)) =>
        o2 shouldBe order2
    }
  }

  "Order Service" should "add order for an existing client" in {
    for {
      _ <- orderService.addOrder(ClientId(1), order2)
      orders <- orderService.listOrdersForClient(ClientId(1))
    } yield inside(orders) {
      case Some(List(o1, o2)) =>
        o1 shouldBe order1
        o2 shouldBe order2
    }
  }
}

