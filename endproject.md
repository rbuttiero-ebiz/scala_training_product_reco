# Projet final

## Description et consignes

  L'objectif de ce projet final est la mise en place d'une application aussi complète que possible de recomandation de produit.
  - Ce projet est un nouveau projet SBT que vous devrez créer (cf. slide sur les templates du chapitre 3), dont les seules dépendances à ajouter sont:
    - ```libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-http" % "10.1.7", "com.typesafe.akka" %% "akka-stream" % "2.5.19")```
  - La méthode main appelera une fonction que je vous fourni plus bas (`bindAndExpose`). Cette fonction attend une fonction `(ClientId) => Future[ProductId]` et expose un endpoint HTTP (`localhost:8080`):
      - Qui pourra recevoir des requêtes GET sur le endpoint `/recommendation/{1234}` avec `1234` étant un ID d'utilisateur (en `Long`)
      - Qui appelera la fonction passée en argument afin de demander une recommandation pour cet utilisateur
      - La seule chose à modifier dans ce code est l'adaptation de celui-ci à vos packages / nom de `class` et la transformation du `Long` reçu en requête en `ClientId` afin qu'elle compile
      - Vous devrez pouvoir tester votre application à la main avec un curl et un `ClientId` existant
  - Il faudra implémenter:
    - Les clients, de deux types, premium et normaux, avec leurs IDs (`ClientId` mentionné plus haut) et la liste de leurs achats (`Product` mentionné plus haut) et dates associées
        - On peut dire qu'il ne font qu'une achat, d'un produit, par jour, et utiliser une `Map[LocalDateTime, Product]`
    - Des produits (`Product` mentionné plus haut), nous n'avons besoin qu'ils n'aient d'autres champs que leurs IDs pour les besoins du projet
    - Un stockage CRUD + `readAll` (une interface !) asynchrone de clients, que vous implémenterez (implémentation concrète) dans une version uniquement en mémoire pour ce projet (une `Map[ClientID, Client]`, mutable, fera l'affaire)
    - Une fonction utilitaire pour remplir ce store aléatoirement au démarrage de l'application afin simuler un historique (cette fonction fera des "create" sur le store au démarrage)
    - Une fonction asynchrone de recommandation (moteur de recommandation) pour le client dont on a reçu l'ID en requête :
      - On cherche, parmi les autres clients, qui est celui qui a acheté le plus d'articles en commun avec notre client (le plus proche voisin)
      - On cherche l'article le plus récent acheté par le plus proche voisin qui n'aurait pas déjà été acheté par le client
      - Si on ne trouve pas d'article qui satisfasse cette condition, on passe au deuxième voisin le plus proche, etc.
      - Si à la fin on a pas trouvé de produit, on renvoie un échec, sinon un succès avec le produit recomandé
      - Si le client est premium, on ne regardera que parmi les clients premium, sinon on regarde parmi tous les clients
    - Votre store et logique métier seront testés unitairement !
   
## Snipets

Ci-après le serveur à coller dans un fichier `Server.scala` au sein d'un package `recommendation` au même niveau que votre fichier contenant le `Main` :

```scala
package recommendation
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import recommendation.model.{ClientId, ProductId}

import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}

object Server {
    def bindAndExpose(recommend: ClientId => Future[ProductId]): Unit = {
      implicit val system = ActorSystem("recommendation-engine")
      implicit val materializer = ActorMaterializer()
      implicit val executionContext = system.dispatcher
    
      val route =
        pathPrefix("recommendation" / LongNumber) { id =>
          get {
            onComplete(recommend(id)) {
              case Success(reco) =>
                complete(
                  HttpEntity(ContentTypes.`text/plain(UTF-8)`, reco.toString))
              case Failure(reason) =>
                failWith(reason)
            }
          }
        }
    
      val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    
      println(
        s"Server online at http://localhost:8080/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
    }
}
```

Et voici à quoi peut ressembler votre main:

```scala

package recommendation

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {

    Await.result(generateHistory(), Duration.Inf) // C'est mal ! Mais cela nous permet de bloquer pour générer l'historique avant le démarrage du serveur
    Server.bindAndExpose(ProductRecommendation.recommend)

}

```