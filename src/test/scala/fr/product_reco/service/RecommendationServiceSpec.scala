package fr.product_reco.service

import fr.product_reco.domain.{ClientId, ProductId}
import fr.product_reco.service.mock.{MockClientService, MockOrderService}
import org.scalatest.{Matchers, _}

import scala.concurrent.ExecutionContext

abstract class RecommendationServiceSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterEach {

  var recoService: RecommendationService = _

  def createRecoService(clientService: ClientService, orderService: OrderService): RecommendationService

  override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def beforeEach() {
    recoService = createRecoService(new MockClientService(), new MockOrderService())
  }

  "Recommendation Service" should "return None for a missing client" in {
    for {
      reco <- recoService.computeRecommendation(ClientId(99))
    } yield reco shouldBe None
  }

  "Recommendation Service" should "return something for an existing client" in {
    for {
      reco <- recoService.computeRecommendation(ClientId(2))
    } yield reco shouldBe Some(ProductId(4))
  }

}

