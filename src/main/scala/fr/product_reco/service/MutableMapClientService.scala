package fr.product_reco.service

import fr.product_reco.domain.{Client, ClientId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MutableMapClientService extends ClientService {

  private val clientsById: scala.collection.mutable.Map[ClientId, Client] = scala.collection.mutable.Map()

  override def listAllClients(): Future[List[Client]] = Future {
    println(s"Listing all clients : ${clientsById.values.toList.mkString(", ")}")
    clientsById.values.toList
  }

  override def getClient(clientId: ClientId): Future[Option[Client]] = Future {
    println(s"Fetching client $clientId -> ${clientsById.get(clientId)}")
    clientsById.get(clientId)
  }

  override def addClient(client: Client): Future[Unit] = Future {
    println(s"Adding client $client")
    clientsById += (client.id -> client)
  }

  override def deleteClient(clientId: ClientId): Future[Unit] = Future {
    println(s"Deleting client $clientId")
    clientsById.remove(clientId)
  }
}
