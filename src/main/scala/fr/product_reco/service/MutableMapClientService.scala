package fr.product_reco.service

import com.typesafe.scalalogging.Logger
import fr.product_reco.domain.{Client, ClientId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MutableMapClientService extends ClientService {

  val logger: Logger = Logger[MutableMapClientService]
  private val clientsById: scala.collection.mutable.Map[ClientId, Client] = scala.collection.mutable.Map()

  override def listAllClients(): Future[List[Client]] = Future {
    logger.debug(s"Listing all clients : ${clientsById.values.toList.mkString(", ")}")
    clientsById.values.toList
  }

  override def getClient(clientId: ClientId): Future[Option[Client]] = Future {
    logger.debug(s"Fetching client $clientId -> ${clientsById.get(clientId)}")
    clientsById.get(clientId)
  }

  override def addClient(client: Client): Future[Unit] = Future {
    logger.info(s"Adding client $client")
    clientsById += (client.id -> client)
  }

  override def deleteClient(clientId: ClientId): Future[Unit] = Future {
    logger.debug(s"Deleting client $clientId")
    clientsById.remove(clientId)
  }
}
