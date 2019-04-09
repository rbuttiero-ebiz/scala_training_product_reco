package fr.product_reco.service

import fr.product_reco.domain.{Client, ClientId}
import org.scalatest.{Matchers, _}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class ClientServiceSpec extends AsyncFlatSpec with Matchers with BeforeAndAfterEach {

  var clientService: ClientService = _

  def createClientService: ClientService

  override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def beforeEach() {
    clientService = createClientService

    val ids = List(ClientId(1), ClientId(2))

    Await.ready(
      Future.sequence(ids.map(id => clientService.addClient(Client(id))))
      , Duration.Inf)
  }

  "Client Service" should "find an existing client" in {
    for {
      client <- clientService.getClient(ClientId(1))
    } yield client shouldBe Some(Client(ClientId(1)))
  }

  "Client Service" should "not find a missing client" in {
    for {
      client <- clientService.getClient(ClientId(99))
    } yield client shouldBe None
  }

  "Client Service" should "list existing clients" in {
    for {
      clients <- clientService.listAllClients()
    } yield clients shouldBe List(Client(ClientId(2)), Client(ClientId(1)))
  }

  "Client Service" should "delete existing clients" in {
    for {
      _ <- clientService.deleteClient(ClientId(2))
      clients <- clientService.listAllClients()
    } yield clients shouldBe List(Client(ClientId(1)))
  }
}

