package fr.product_reco.service.mock

import fr.product_reco.domain._
import fr.product_reco.service.ClientService

import scala.concurrent.Future

class MockClientService extends ClientService {

  val client1: Client = Client(ClientId(1))
  val client2: Client = Client(ClientId(2))
  val client3: Client = Client(ClientId(3))
  val clients: Map[ClientId, Client] = Map(
    ClientId(1) -> client1,
    ClientId(2) -> client2,
    ClientId(3) -> client3)

  override def listAllClients(): Future[List[Client]] = Future.successful(clients.values.toList)

  override def getClient(clientId: ClientId): Future[Option[Client]] = Future.successful(clients.get(clientId))

  override def addClient(client: Client): Future[Unit] = Future.successful()

  override def deleteClient(clientId: ClientId): Future[Unit] = Future.successful()
}
