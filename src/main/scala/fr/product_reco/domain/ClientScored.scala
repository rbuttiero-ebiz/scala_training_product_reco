package fr.product_reco.domain

final case class ClientScored(client:Client, score: Int, sameCount: Int, differentCount: Int, hasMoreProducts: Boolean) {

}
