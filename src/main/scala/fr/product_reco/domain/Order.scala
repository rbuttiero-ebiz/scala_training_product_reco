package fr.product_reco.domain

import java.time.LocalDateTime

final case class Order(id: OrderId, date: LocalDateTime, items: List[OrderItem])
