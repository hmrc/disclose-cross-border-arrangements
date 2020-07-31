package repositories

import models.SubmissionDetails
import org.joda.time.DateTime
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import suite.MongoSuite

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionDetailsRepositorySpec
  extends FreeSpec with MustMatchers with MongoSuite with ScalaFutures with IntegrationPatience {

  "SubmissionDetailsRepository" - {

    val submissionDetails = SubmissionDetails(
      enrolmentID = "enrolmentID",
      submissionTime = DateTime.now(),
      fileName = "fileName.xml",
      arrangementID = Some("GBA20200601AAA000"),
      disclosureID = Some("GBD20200601AAA000"),
      importInstruction = "DAC6ADD",
      initialDisclosureMA = false)

    "must get submission details" in {

      val app: Application = new GuiceApplicationBuilder().build()
      val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

      running(app) {

        database.flatMap(_.drop()).futureValue
        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.getSubmissionDetails("GBD20200601AAA000")) {
          _ mustBe Some(submissionDetails)
        }

      }
    }

    "must retrieve submission details history - latest submissions first" in {
      val olderSubmissionDetails = submissionDetails.copy(fileName = "another-file.xml", submissionTime = DateTime.now().minusDays(1))
      val diffSubmissionDetails = submissionDetails.copy(enrolmentID = "diffEnrolmentID", fileName = "diff-file.xml")
      val app: Application = new GuiceApplicationBuilder().build()
      val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

      running(app) {

        database.flatMap(_.drop()).futureValue
        await(repo.storeSubmissionDetails(olderSubmissionDetails))
        await(repo.storeSubmissionDetails(diffSubmissionDetails))
        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.retrieveSubmissionHistory("enrolmentID")) {
          _ mustEqual List(submissionDetails, olderSubmissionDetails)
        }
      }
    }

    "must store submission details" in {
      val app: Application = new GuiceApplicationBuilder().build()
      val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

      running(app) {

        database.flatMap(_.drop()).futureValue

        whenReady(repo.storeSubmissionDetails(submissionDetails)) {
          _ mustEqual true
        }
      }
    }

  }

}