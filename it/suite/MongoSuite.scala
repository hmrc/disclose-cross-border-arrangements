package suite

import com.typesafe.config.ConfigFactory
import org.scalatest._
import play.api.Configuration
import reactivemongo.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object MongoSuite {

  private lazy val config = Configuration(ConfigFactory.load(System.getProperty("config.resource")))

  private lazy val parsedUri = Future.fromTry {
    MongoConnection.parseURI(config.get[String]("mongodb.uri"))
  }

  lazy val connection: Future[MongoConnection] =
    parsedUri.flatMap(parsedURI => Future.fromTry(MongoDriver().connection(parsedURI, name = None, strictUri = false)))
}

trait MongoSuite {
  self: TestSuite =>

  def database: Future[DefaultDB] =
    for {
      uri        <- MongoSuite.parsedUri
      connection <- MongoSuite.connection
      database   <- connection.database(uri.db.get)
    } yield database
}
