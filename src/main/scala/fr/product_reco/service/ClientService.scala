package fr.product_reco.service

import fr.product_reco.domain.{Client, ClientId}

import scala.concurrent.Future

trait ClientService {

  def listAllClients(): Future[List[Client]]

  def getClient(clientId: ClientId): Future[Option[Client]]

  def addClient(client: Client): Future[Unit]

  def deleteClient(clientId: ClientId): Future[Unit]

}
