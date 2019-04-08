package fr.product_reco.domain

final case class OrderItem(id: OrderItemId, product: Product, qty: Int)
