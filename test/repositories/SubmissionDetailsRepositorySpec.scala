/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories

import base.SpecBase
import models.SubmissionDetails
import models.upscan.UploadId
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.result.DeleteResult
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Matchers.convertToAnyShouldWrapper
import play.api.test.Helpers.{await, _}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.reflect.ClassTag

class SubmissionDetailsRepositorySpec extends SpecBase with BeforeAndAfterEach {
  lazy val submissionDetailsRep = app.injector.instanceOf[SubmissionDetailsRepository]
  val uploadId = UploadId(UUID.randomUUID().toString)
  val submissionDetails = SubmissionDetails(enrolmentID = "ID",
    submissionTime = LocalDateTime.now(),
    fileName = "file",
    arrangementID = Some("arrangementId"),
    disclosureID = Some("disclossureId"),
    importInstruction = "New",
    initialDisclosureMA = false,
    messageRefId = UUID.randomUUID().toString)

  override protected def beforeEach(): Unit = {
    await(removeAll(submissionDetailsRep))
    super.beforeEach()
  }

  "Insert" - {
    "must insert Submission Details" in {
      val sd = submissionDetailsRep.storeSubmissionDetails(submissionDetails)
      whenReady(sd) { result =>
        result shouldBe true
      }
    }
  }

  "SubmissionDetailsRepository" - {

    val arrangementID = "GBA20200904AAAAAA"
    val disclosureID = "GBD20200601AAA000"
    val enrolmentID = "XADAC0001234567"

    val date = LocalDateTime.now()

    val submissionDetails = SubmissionDetails(
      enrolmentID = enrolmentID,
      submissionTime = date,
      fileName = "fileName.xml",
      arrangementID = Some(arrangementID),
      disclosureID = Some(disclosureID),
      importInstruction = "Add",
      initialDisclosureMA = false,
      messageRefId = "GBXADAC0001234567AAA00101")

    val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

    "calling getSubmissionDetails" - {
      "must get submission details" in {


        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.getSubmissionDetails(disclosureID)) {
          _ mustBe Some(submissionDetails)
        }


      }
    }

    "calling retrieveFirstDisclosureForArrangementId" - {
      "must retrieve submission details for the first disclosure for arrangement ID" in {
        val firstSubmissionDetails = submissionDetails.copy(importInstruction = "New", initialDisclosureMA = true)
        val secondSubmissionDetails = submissionDetails.copy(submissionTime = LocalDateTime.now().plusDays(1))

        await(repo.storeSubmissionDetails(firstSubmissionDetails))
        await(repo.storeSubmissionDetails(secondSubmissionDetails))

        whenReady(repo.retrieveFirstDisclosureForArrangementId(arrangementID)) {
          _ mustBe Some(firstSubmissionDetails)
        }
      }
      "must return None if there is no first disclosure for arrangement ID" in {
        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]


        whenReady(repo.retrieveFirstDisclosureForArrangementId(arrangementID)) {
          _ mustBe None
        }
      }
    }

    "calling retrieveSubmissionHistory" - {
      "must retrieve submission details history - latest submissions first" in {
        val olderSubmissionDetails = submissionDetails.copy(fileName = "another-file.xml", submissionTime = LocalDateTime.now().minusDays(1))
        val diffSubmissionDetails = submissionDetails.copy(enrolmentID = "diffEnrolmentID", fileName = "diff-file.xml")

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(olderSubmissionDetails))
        await(repo.storeSubmissionDetails(diffSubmissionDetails))
        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.retrieveSubmissionHistory(enrolmentID)) {
          _ mustEqual List(submissionDetails, olderSubmissionDetails)
        }
      }
    }

    "calling countNoOfPreviousSubmissions" - {
      "must correctly count submission details" in {

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.countNoOfPreviousSubmissions(enrolmentID)) {
          _ mustEqual 1
        }
      }

      "must correctly return a zero count when user has not submitted" in {

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]


        whenReady(repo.countNoOfPreviousSubmissions(enrolmentID)) {
          _ mustEqual 0
        }
      }
    }

    "calling storeSubmissionDetails" - {
      "must store submission details" in {

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]


        whenReady(repo.storeSubmissionDetails(submissionDetails)) {
          _ mustEqual true
        }
      }
    }

    "calling searchSubmissions" - {

      "must search for the arrangement ID and return a list of matching submissions" in {
        val submissionDetailsSameArrID = submissionDetails.copy(importInstruction = "Delete")
        val submissionDetailsOther = submissionDetails.copy(arrangementID = Some("GBA20200904AAABBB"))

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))
        await(repo.storeSubmissionDetails(submissionDetailsSameArrID))
        await(repo.storeSubmissionDetails(submissionDetailsOther))

        whenReady(repo.searchSubmissions(arrangementID)) {
          _ mustEqual List(submissionDetails, submissionDetailsSameArrID)
        }
      }

      "must search for the disclosure ID and return a list of matching submissions" in {
        val submissionDetailsSameDiscID = submissionDetails.copy(importInstruction = "Replace")
        val submissionDetailsOther = submissionDetails.copy(disclosureID = Some("GBD20200601BBB000"))

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))
        await(repo.storeSubmissionDetails(submissionDetailsSameDiscID))
        await(repo.storeSubmissionDetails(submissionDetailsOther))

        whenReady(repo.searchSubmissions(disclosureID)) {
          _ mustEqual List(submissionDetails, submissionDetailsSameDiscID)
        }
      }

      "must search for the message ref ID and return a list of matching submissions" in {
        val submissionDetailsOther = submissionDetails.copy(messageRefId = "GBXADAC0001234567AAA00102")

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))
        await(repo.storeSubmissionDetails(submissionDetailsOther))

        whenReady(repo.searchSubmissions("GBXADAC0001234567AAA00101")) {
          _ mustEqual List(submissionDetails)
        }
      }

      "must search for the partial search string and return a list of matching submissions" in {
        val submissionDetailsSameFileName = submissionDetails.copy(disclosureID = Some("GBD20200601BBB000"))
        val submissionDetailsOther = submissionDetails.copy(disclosureID = Some("GBD20200601CCC000"))

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))
        await(repo.storeSubmissionDetails(submissionDetailsSameFileName))
        await(repo.storeSubmissionDetails(submissionDetailsOther))

        whenReady(repo.searchSubmissions("GBA")) {
          _ mustEqual List(submissionDetails, submissionDetailsSameFileName, submissionDetailsOther)
        }
      }

      "must return an empty if there's no match" in {

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.searchSubmissions("other.xml")) {
          _ mustEqual List()
        }
      }
    }

    "doesDisclosureIdMatchEnrolmentID" - {
      "must return true if disclosure ID and enrolment ID match a submission" in {

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.doesDisclosureIdMatchEnrolmentID(disclosureID, enrolmentID)) {
          _ mustEqual true
        }
      }

      "must return false if disclosure ID and enrolment ID don't match a submission" in {

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.doesDisclosureIdMatchEnrolmentID(disclosureID, "XADAC0001111111")) {
          _ mustEqual false
        }
      }
    }

    "doesDisclosureIdMatchArrangementID" - {
      "must return true if disclosure ID and arrangement ID match a submission" in {

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.doesDisclosureIdMatchArrangementID(disclosureID, arrangementID)) {
          _ mustEqual true
        }
      }

      "must return false if disclosure ID and arrangement ID don't match a submission" in {

        val repo: SubmissionDetailsRepository = app.injector.instanceOf[SubmissionDetailsRepository]

        await(repo.storeSubmissionDetails(submissionDetails))

        whenReady(repo.doesDisclosureIdMatchArrangementID(disclosureID, "GBA20200904BBBBBB")) {
          _ mustEqual false
        }
      }
    }
  }


  def removeAll[T: ClassTag](connection: PlayMongoRepository[T]): Future[DeleteResult] =
    connection.collection.deleteMany(BsonDocument()).toFuture

}