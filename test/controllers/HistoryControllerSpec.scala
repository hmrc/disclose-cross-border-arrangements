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

package controllers

import java.time.LocalDateTime

import base.SpecBase
import controllers.auth.{AuthAction, FakeAuthAction}
import generators.ModelGenerators
import models.{SubmissionDetails, SubmissionHistory}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, route, status, _}
import repositories.SubmissionDetailsRepository

import scala.concurrent.Future

class HistoryControllerSpec extends SpecBase
  with ScalaCheckPropertyChecks
  with ModelGenerators
  with BeforeAndAfterEach {

  val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]

  override def beforeEach(): Unit = reset(mockSubmissionDetailsRepository)

  val arrangementID = "GBA20200904AAAAAA"
  val disclosureID = "GBD20200904AAAAAA"
  val messageRefId = "GB1234567"
  val initialSubmissionDetails: SubmissionDetails =
    SubmissionDetails(
      enrolmentID = "enrolmentID",
      submissionTime = LocalDateTime.now(),
      fileName = "fileName.xml",
      arrangementID = Some(arrangementID),
      disclosureID = Some(disclosureID),
      importInstruction = "New",
      initialDisclosureMA = true,
      messageRefId
    )

  val application: Application =
    applicationBuilder()
      .overrides(
        bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository),
        bind[AuthAction].to[FakeAuthAction]
      ).build()

  "noOfPreviousSubmissions" - {
    "must return ok with noOfPreviousSubmissions" in {
      when(mockSubmissionDetailsRepository.countNoOfPreviousSubmissions(any()))
        .thenReturn(Future.successful(2 : Long))

      val request = FakeRequest(GET, routes.HistoryController.noOfPreviousSubmissions("123").url)

      val result = route(application, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(2)
    }
  }

  "submissionDetails" - {
    "must return ok with submission details" in {
      forAll(listWithMaxLength[SubmissionDetails](15)){
        details =>
          when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any()))
            .thenReturn(Future.successful(details))

          val request = FakeRequest(GET, routes.HistoryController.submissionDetails("123").url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(SubmissionHistory(details))
      }
    }

    "must return Internal Service Error when there is a problem fetching results" in {
      when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any()))
            .thenReturn(Future.failed(new Exception))

      val request = FakeRequest(GET, routes.HistoryController.submissionDetails("123").url)

      val result = route(application, request).value

      status(result) mustEqual INTERNAL_SERVER_ERROR

    }
  }

  "disclosureDetails" - {
    "must return OK with submission details" in {
      forAll(listWithMaxLength[SubmissionDetails](15)){
        details =>
          when(mockSubmissionDetailsRepository.getSubmissionDetails(any()))
            .thenReturn(Future.successful(details.headOption))

          val request = FakeRequest(GET, routes.HistoryController.disclosureDetails("123").url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsJson(result) mustEqual Json.toJson(details.headOption)
      }
    }

    "must return Internal Service Error when there is a problem fetching results" in {
      when(mockSubmissionDetailsRepository.getSubmissionDetails(any()))
        .thenReturn(Future.failed(new Exception))

      val request = FakeRequest(GET, routes.HistoryController.disclosureDetails("123").url)

      val result = route(application, request).value

      status(result) mustEqual INTERNAL_SERVER_ERROR
    }
  }

  "retrieveFirstDisclosure" - {
    "must return a NOT_FOUND if there is no first disclosure" in {
      when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId(""))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(GET, routes.HistoryController.retrieveFirstDisclosure("").url)

      val result = route(application, request).value

      status(result) mustEqual NOT_FOUND
    }

    "must return an OK if there is a first disclosure available for the given arrangement ID" in {
      when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId(arrangementID))
        .thenReturn(Future.successful(Some(initialSubmissionDetails)))

      val request = FakeRequest(GET, routes.HistoryController.retrieveFirstDisclosure(arrangementID).url)

      val result = route(application, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(initialSubmissionDetails)
    }

  }

  "isMarketableArrangement" - {

    "must return a NOT_FOUND if there is no first disclosure" in {
      when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId(""))
        .thenReturn(Future.successful(None))

      val request = FakeRequest(GET, routes.HistoryController.isMarketableArrangement("").url)

      val result = route(application, request).value

      status(result) mustEqual NOT_FOUND
    }

    "must return an OK with the answer if there is a first disclosure available for the given arrangement ID" in {
      when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId(arrangementID))
        .thenReturn(Future.successful(Some(initialSubmissionDetails)))

      val request = FakeRequest(GET, routes.HistoryController.isMarketableArrangement(arrangementID).url)

      val result = route(application, request).value

      status(result) mustEqual OK
      contentAsJson(result).as[Boolean] mustEqual true
    }

  }

  "searchSubmissions" - {
    "must return an OK with the submission history if the search has found submissions" in {
      val search = "fileName"
      val submissionDetailsList = List(initialSubmissionDetails)

      when(mockSubmissionDetailsRepository.searchSubmissions(search))
        .thenReturn(Future.successful(submissionDetailsList))

      val request = FakeRequest(GET, routes.HistoryController.searchSubmissions(search).url)

      val result = route(application, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(SubmissionHistory(submissionDetailsList))
    }

    "must return a NOT_FOUND if mongo fails" in {
      val search = "fileName"

      when(mockSubmissionDetailsRepository.searchSubmissions(search))
        .thenReturn(Future.failed(new Exception("")))

      val request = FakeRequest(GET, routes.HistoryController.searchSubmissions(search).url)

      val result = route(application, request).value

      status(result) mustEqual NOT_FOUND
    }

  }

}
