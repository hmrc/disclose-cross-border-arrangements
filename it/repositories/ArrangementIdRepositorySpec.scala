package repositories

import models.ArrangementId
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import suite.MongoSuite

import scala.concurrent.ExecutionContext.Implicits.global

class ArrangementIdRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with OptionValues
    with MongoSuite
    with IntegrationPatience {

  "Arrangement Id Repository" - {

    "must store arrangement Id correctly" in {

      val app = new GuiceApplicationBuilder().build()

      running(app) {

        val repo = app.injector.instanceOf[ArrangementIdRepository]

        database.flatMap(_.drop()).futureValue

        val arrangementId = ArrangementId(dateString = "date", suffix = "suffix")
        whenReady (repo.storeArrangementId(arrangementId).flatMap(_ => repo.doesArrangementIdExist(arrangementId))) { result =>
          result mustBe true
        }
      }
    }
  }
}
