package fr.product_reco.service

import java.time.LocalDateTime

import com.typesafe.scalalogging.Logger
import fr.product_reco.domain.{Order, _}
import scalaz.OptionT
import scalaz.Scalaz._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.{max, min}

class ClosestClientRecommendationService(clientService: ClientService, orderService: OrderService) extends RecommendationService {

  val logger: Logger = Logger[ClosestClientRecommendationService]

  override def computeRecommendation(clientId: ClientId): Future[Option[ProductId]] = {
    logger.info(s"Computing recommendation for $clientId")
    val recommendedProduct: OptionT[Future, ProductId] = for {
      client <- OptionT(clientService.getClient(clientId))
      closestClient <- OptionT(searchClosestClient(client))
      recommendedProduct <- OptionT(extractRecommendedProduct(client, closestClient.client))
    } yield recommendedProduct

    recommendedProduct.run
  }

  def searchClosestClient(refClient: Client): Future[Option[ClientScored]] = {
    logger.info(s"Searching closest client for $refClient")
    val futureAllClients: Future[List[Client]] = clientService.listAllClients()
    val futureRefProductQuantities: Future[Option[Map[ProductId, Int]]] = extractOrderedProducts(refClient)

    // TODO : Which is better ?
    // filter -> sort -> head
    // vs sort -> collectFirst
    // vs filter -> reduce that keeps the best score ?

    /*
    for {
      refProductQuantities <- futureRefProductQuantities
      allClients <- futureAllClients
      allScoredClients <- Future.sequence(allClients.map(scoreClient(refProductQuantities, _)))
    } yield allScoredClients
      .filter(filterCloseClient(refClient, _))
      .sortWith(_.score > _.score)
      .headOption
      */

    val closestClient = for {
      refProductQuantities <- futureRefProductQuantities
      allClients <- futureAllClients
      allScoredClients <- Future.sequence(allClients.map(scoreClient(refClient, refProductQuantities, _)))
    } yield allScoredClients
      .filter(filterCloseClient(refClient, _))
    match {
      case l if l.nonEmpty =>
        val closestClientFound = l.reduce((p1, p2) => if (p1.score >= p2.score) p1 else p2)
        logger.info(s"Closest client for $refClient : $closestClientFound")
        Some(closestClientFound)
      case _ => None
    }

    closestClient
  }

  def scoreClient(refClient: Client, refProductQuantities: Option[Map[ProductId, Int]], compareClient: Client): Future[ClientScored] = {
    logger.debug(s"Searching client for $refClient : $compareClient")
    val score = for {
      compareProductQuantities <- extractOrderedProducts(compareClient)
    } yield scoreClientProducts(refClient, compareClient, refProductQuantities.getOrElse(Map()), compareProductQuantities.getOrElse(Map()))

    score
  }

  def extractOrderedProducts(client: Client): Future[Option[Map[ProductId, Int]]] = {
    val extractedProducts: OptionT[Future, Map[ProductId, Int]] = for {
      orderList <- OptionT(orderService.listOrdersForClient(client.id))
    } yield extractProducts(orderList)

    extractedProducts.run
  }

  def extractOrderedProductsWithLatestDates(client: Client): Future[Option[Map[ProductId, LocalDateTime]]] = {
    val extractedProducts: OptionT[Future, Map[ProductId, LocalDateTime]] = for {
      orderList <- OptionT(orderService.listOrdersForClient(client.id))
    } yield extractProductsWithLatestDates(orderList)

    extractedProducts.run
  }

  def extractProductsWithLatestDates(orders: List[Order]): Map[ProductId, LocalDateTime] = {
    implicit val localDateTimeOrdering: Ordering[LocalDateTime] = _ compareTo _

    orders
      .flatMap(order => order.items.map(item => (item.product.id, order.date))) //Seq[(ProductId, LocalDateTime)]
      .groupBy(_._1) // Map[ProductId, List[(ProductId, LocalDateTime)]]
      .mapValues(_.map(_._2).max) // Keep the most recent date
  }

  def extractProducts(orders: List[Order]): Map[ProductId, Int] = {
    orders
      .flatMap(_.items) // Seq[OrderItem]
      .groupBy(_.product.id) // Map[ProductId, List[OrderItem]]
      .mapValues(_.map(_.qty).min) // And finally sum quantities in each List[OrderItem]
  }

  def scoreClientProducts(refClient: Client, compareClient: Client, refProductQuantities: Map[ProductId, Int], compareProductQuantities: Map[ProductId, Int]): ClientScored = {
    val commonProductIds = refProductQuantities.keySet.intersect(compareProductQuantities.keySet)
    val onlyInRefProductIds = refProductQuantities.keySet.diff(compareProductQuantities.keySet)
    val onlyInCompareProductIds = compareProductQuantities.keySet.diff(refProductQuantities.keySet)

    /*
    logger.debug(s"Scoring for $refClient : $compareClient : refProductQuantities = $refProductQuantities")
    logger.debug(s"Scoring for $refClient : $compareClient : compareProductQuantities = $compareProductQuantities")
    logger.debug(s"Scoring for $refClient : $compareClient : commonProductIds = $commonProductIds")
    logger.debug(s"Scoring for $refClient : $compareClient : onlyInRefProductIds = $onlyInRefProductIds")
    logger.debug(s"Scoring for $refClient : $compareClient : onlyInCompareProductIds = $onlyInCompareProductIds")
     */

    // sum of the quantities of products that are in both maps
    val sameCount = commonProductIds
      .map(productId => productId -> min(refProductQuantities(productId), compareProductQuantities(productId)))
      .toMap.values.sum

    // sum of the quantities for products that are not in both maps
    val differentCount_onlyInRef = onlyInRefProductIds
      .map(productId => productId -> refProductQuantities(productId))
      .toMap.values.sum

    // sum of the quantities for products that are not in both maps
    val differentCount_onlyInCompare = onlyInCompareProductIds
      .map(productId => productId -> compareProductQuantities(productId))
      .toMap.values.sum

    // sum of the exceeding quantities for products that are in both maps
    val differentCount_inBoth = commonProductIds
      .map(productId => productId -> (
        max(refProductQuantities(productId), compareProductQuantities(productId)) -
          min(refProductQuantities(productId), compareProductQuantities(productId))
        ))
      .toMap.values.sum

    val differentCount = differentCount_onlyInRef + differentCount_onlyInCompare + differentCount_inBoth

    val hasMoreProducts = differentCount_onlyInCompare > 0

    val score = sameCount
    val clientScored = ClientScored(compareClient, score, sameCount, differentCount, hasMoreProducts)

    logger.debug(s"Client scored for $refClient : $compareClient = $clientScored")
    clientScored
  }

  def filterCloseClient(client: Client, clientScored: ClientScored): Boolean = {
    client.id != clientScored.client.id && // remove the original client
      clientScored.hasMoreProducts && // remove clients without other products
      (client match {
        case PremiumClient(_) => filterClosePremiumClient(client, clientScored)
        case _ => filterCloseStandardClient(client, clientScored)
      })
  }

  def filterCloseStandardClient(client: Client, clientScored: ClientScored): Boolean = {
    // no special rule for StandardClient
    true
  }

  def filterClosePremiumClient(client: Client, clientScored: ClientScored): Boolean = {
    // only match PremiumClient
    clientScored.client.isInstanceOf[PremiumClient]
  }

  def extractRecommendedProduct(client: Client, closestClient: Client): Future[Option[ProductId]] = {
    val futureProductQuantities: Future[Option[Map[ProductId, Int]]] = extractOrderedProducts(client)
    val futureClosestProductQuantities: Future[Option[Map[ProductId, LocalDateTime]]] = extractOrderedProductsWithLatestDates(closestClient)

    for {
      productQuantities <- futureProductQuantities
      closestProductQuantities <- futureClosestProductQuantities
    } yield closestProductQuantities
      .getOrElse(Map()).keySet.diff(productQuantities.getOrElse(Map()).keySet)
      .toList
    match {
      case l if l.nonEmpty =>
        val recommendedProductId =
          l.map(productId => (productId, closestProductQuantities.get(productId))) // Future[List[(ProductId, LocalDateTime)]]
            .reduce((p1, p2) => if (p1._2.isAfter(p2._2)) p1 else p2)
            ._1
        logger.info(s"Recommended product for $client : $recommendedProductId")
        Some(recommendedProductId)
      case _ => None
    }
  }

}
