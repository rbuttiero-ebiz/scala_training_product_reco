package fr.product_reco.service

class MutableMapOrderServiceSpec extends OrderServiceSpec {

  override def createOrderService: OrderService = new MutableMapOrderService()
}
