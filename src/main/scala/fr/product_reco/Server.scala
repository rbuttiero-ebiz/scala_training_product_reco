package fr.product_reco

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import fr.product_reco.domain.{ClientId, ProductId}


import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}

object Server {

  def bindAndExpose(recommend: ClientId => Future[Option[ProductId]]): Unit = {
    implicit val system = ActorSystem("recommendation-engine")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val route =
      pathPrefix("recommendation" / LongNumber) { id =>
        get {
          val clientId = ClientId(id)
          onComplete(recommend(clientId)) {
            case Success(Some(reco)) => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, reco.toString))
            case Success(None) => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Not found"))
            case Failure(reason) => failWith(reason)
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
