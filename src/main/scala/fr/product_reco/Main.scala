package fr.product_reco

import java.time.LocalDateTime

import fr.product_reco.domain._
import fr.product_reco.service._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main extends App {


  val clientService: ClientService = new MutableMapClientService()
  val orderService: OrderService = new MutableMapOrderService()
  val service: RecommendationService = new ClosestClientRecommendationService(clientService, orderService)

  Await.result(generateHistory(), Duration.Inf)

  def generateHistory(): Future[Unit] = Future {
    val r = scala.util.Random
    val clientIds = 1 to 100

    clientIds.foreach(clientId => {
      val client = Client(ClientId(clientId.toLong))
      clientService.addClient(client)

      val orderCounter = 0 to r.nextInt(10)
      orderCounter.foreach(orderCounter => {
        val orderId = OrderId(clientId * 1000 + orderCounter.toLong)

        val itemCounters = 1 to r.nextInt(10)
        val items = itemCounters.map(itemCounter => {
          val itemId = OrderItemId(orderId.id * 1000 + itemCounter.toLong)
          val productId = ProductId(r.nextInt(20))
          OrderItem(itemId, Product(productId), r.nextInt(10))
        }).toList

        val order = Order(orderId, LocalDateTime.now(), items)
        orderService.addOrder(client.id, order)
      })
    })
  }

  Server.bindAndExpose(service.computeRecommendation)

}
