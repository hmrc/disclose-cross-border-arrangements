package repositories

import models.DisclosureId
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import suite.MongoSuite

import scala.concurrent.ExecutionContext.Implicits.global

class DisclosureIdRepositorySpec
  extends FreeSpec
    with MustMatchers
    with ScalaFutures
    with OptionValues
    with MongoSuite
    with IntegrationPatience {

  "Disclosure Id Repository" - {

    "must store disclosure Id correctly" in {

      val app = new GuiceApplicationBuilder().build()

      running(app) {

        val repo = app.injector.instanceOf[DisclosureIdRepository]

        database.flatMap(_.drop()).futureValue

        val disclosureId = DisclosureId(dateString = "date", suffix = "suffix")
        whenReady (repo.storeDisclosureId(disclosureId).flatMap(_ => repo.doesDisclosureIdExist(disclosureId))) { result =>
          result mustBe true
        }
      }
    }
  }
}
