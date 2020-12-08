package repositories


import java.time.LocalDateTime

import models.SubmissionDetails
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

    val arrangementID = "GBA20200904AAAAAA"
    val disclosureID = "GBD20200601AAA000"

    val submissionDetails = SubmissionDetails(
      enrolmentID = "enrolmentID",
      submissionTime = LocalDateTime.now(),
      fileName = "fileName.xml",
      arrangementID = Some(arrangementID),
      disclosureID = Some(disclosureID),
      importInstruction = "Add",
      initialDisclosureMA = false,
      messageRefId = "GB0000000XXX")

    "calling getSubmissionDetails" - {
      "must get submission details" in {

        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(submissionDetails))

          whenReady(repo.getSubmissionDetails(disclosureID)) {
            _ mustBe Some(submissionDetails)
          }

        }
      }
    }

     "calling retrieveFirstDisclosureForArrangementId" - {
      "must retrieve submission details for the first disclosure for arrangement ID" in {
        val firstSubmissionDetails = submissionDetails.copy(importInstruction = "New", initialDisclosureMA = true)
        val secondSubmissionDetails = submissionDetails.copy(submissionTime = LocalDateTime.now().plusDays(1))
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(firstSubmissionDetails))
          await(repo.storeSubmissionDetails(secondSubmissionDetails))

          whenReady(repo.retrieveFirstDisclosureForArrangementId(arrangementID)) {
            _ mustBe Some(firstSubmissionDetails)
          }
        }
      }

      "must return None if there is no first disclosure for arrangement ID" in {
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue

          whenReady(repo.retrieveFirstDisclosureForArrangementId(arrangementID)) {
            _ mustBe None
          }
        }
      }
    }

    "calling retrieveSubmissionHistory" - {
      "must retrieve submission details history - latest submissions first" in {
        val olderSubmissionDetails = submissionDetails.copy(fileName = "another-file.xml", submissionTime = LocalDateTime.now().minusDays(1))
        val diffSubmissionDetails = submissionDetails.copy(enrolmentID = "diffEnrolmentID", fileName = "diff-file.xml")
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(olderSubmissionDetails))
          await(repo.storeSubmissionDetails(diffSubmissionDetails))
          await(repo.storeSubmissionDetails(submissionDetails))

          whenReady(repo.retrieveSubmissionHistory("enrolmentID")) {
            _ mustEqual List(submissionDetails, olderSubmissionDetails)
          }
        }
      }
    }

    "calling countNoOfPreviousSubmissions" - {
      "must correctly count submission details" in {
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(submissionDetails))

          whenReady(repo.countNoOfPreviousSubmissions("enrolmentID")) {
            _ mustEqual 1
          }
        }
      }

      "must correctly return a zero count when user has not submitted" in {
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue

          whenReady(repo.countNoOfPreviousSubmissions("enrolmentID")) {
            _ mustEqual 0
          }
        }
      }
    }

    "calling storeSubmissionDetails" - {
      "must store submission details" in {
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue

          whenReady(repo.storeSubmissionDetails(submissionDetails)) {
            _ mustEqual true
          }
        }
      }
    }

    "calling searchSubmissions" - {

      "must search for the arrangement ID and return a list of matching submissions" in {
        val submissionDetailsSameArrID = submissionDetails.copy(importInstruction = "Delete")
        val submissionDetailsOther = submissionDetails.copy(arrangementID = Some("GBA20200904AAABBB"))
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(submissionDetails))
          await(repo.storeSubmissionDetails(submissionDetailsSameArrID))
          await(repo.storeSubmissionDetails(submissionDetailsOther))

          whenReady(repo.searchSubmissions(arrangementID)) {
            _ mustEqual List(submissionDetails, submissionDetailsSameArrID)
          }
        }
      }

      "must search for the disclosure ID and return a list of matching submissions" in {
        val submissionDetailsSameDiscID = submissionDetails.copy(importInstruction = "Replace")
        val submissionDetailsOther = submissionDetails.copy(disclosureID = Some("GBD20200601BBB000"))
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(submissionDetails))
          await(repo.storeSubmissionDetails(submissionDetailsSameDiscID))
          await(repo.storeSubmissionDetails(submissionDetailsOther))

          whenReady(repo.searchSubmissions(disclosureID)) {
            _ mustEqual List(submissionDetails, submissionDetailsSameDiscID)
          }
        }
      }

      "must search for the file name and return a list of matching submissions" in {
        val submissionDetailsSameFileName = submissionDetails.copy(disclosureID = Some("GBD20200601BBB000"))
        val submissionDetailsOther = submissionDetails.copy(fileName = "other.xml")
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(submissionDetails))
          await(repo.storeSubmissionDetails(submissionDetailsSameFileName))
          await(repo.storeSubmissionDetails(submissionDetailsOther))

          whenReady(repo.searchSubmissions("fileName.xml")) {
            _ mustEqual List(submissionDetails, submissionDetailsSameFileName)
          }
        }
      }

      "must search for the partial search string and return a list of matching submissions" in {
        val submissionDetailsSameFileName = submissionDetails.copy(disclosureID = Some("GBD20200601BBB000"))
        val submissionDetailsOther = submissionDetails.copy(disclosureID = Some("GBD20200601CCC000"))
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(submissionDetails))
          await(repo.storeSubmissionDetails(submissionDetailsSameFileName))
          await(repo.storeSubmissionDetails(submissionDetailsOther))

          whenReady(repo.searchSubmissions("GBA")) {
            _ mustEqual List(submissionDetails, submissionDetailsSameFileName, submissionDetailsOther)
          }
        }
      }

      "must return an empty if there's no match" in {
        val app: Application = new GuiceApplicationBuilder().build()

        running(app) {
          val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

          database.flatMap(_.drop()).futureValue
          await(repo.storeSubmissionDetails(submissionDetails))

          whenReady(repo.searchSubmissions("other.xml")) {
            _ mustEqual List()
          }
        }
      }
    }

  }

}
