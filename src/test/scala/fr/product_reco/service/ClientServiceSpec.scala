package fr.product_reco.service

import fr.product_reco.domain.{ClientId, PremiumClient, StandardClient}
import org.scalatest.{Matchers, _}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class ClientServiceSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterEach {

  var clientService: ClientService = _

  def createClientService: ClientService

  override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def beforeEach() {
    clientService = createClientService

    val clients = List(
      StandardClient(ClientId(1)),
      PremiumClient(ClientId(2))
    )

    Await.ready(
      Future.sequence(clients.map(clientService.addClient(_)))
      , Duration.Inf)
  }

  "Client Service" should "find an existing client" in {
    for {
      client <- clientService.getClient(ClientId(1))
    } yield client shouldBe Some(StandardClient(ClientId(1)))
  }

  "Client Service" should "not find a missing client" in {
    for {
      client <- clientService.getClient(ClientId(99))
    } yield client shouldBe None
  }

  "Client Service" should "list existing clients" in {
    for {
      clients <- clientService.listAllClients()
    } yield clients shouldBe List(PremiumClient(ClientId(2)), StandardClient(ClientId(1)))
  }

  "Client Service" should "delete existing clients" in {
    for {
      _ <- clientService.deleteClient(ClientId(2))
      clients <- clientService.listAllClients()
    } yield clients shouldBe List(StandardClient(ClientId(1)))
  }
}

