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

import base.SpecBase
import controllers.auth.{AuthAction, FakeAuthAction}
import models.{ArrangementId, DisclosureId}
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status, _}
import play.api.{Application, Configuration}
import services.{ArrangementIDNotFound, DisclosureIDNotFound, IdService, IdsCorrect, IdsInDifferentSubmissions, IdsNotFound}

import scala.concurrent.Future

class IdControllerSpec extends SpecBase with ScalaCheckPropertyChecks {

  val mockIdService: IdService = mock[IdService]

  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false"))
    .overrides(
      bind[IdService].toInstance(mockIdService),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  val validArrangementId: ArrangementId = ArrangementId(dateString = "20200601", suffix = "A1B1C1")
  val validDisclosureId: DisclosureId   = DisclosureId(dateString = "20210101", suffix = "A1B1C1")
  val enrolmentID: String               = "XADAC0001234567"

  "IdController" - {
    "verifyArrangementId" - {
      "return 204 for valid existing arrangement id" in {

        when(mockIdService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        val request =
          FakeRequest(GET, routes.IdController.verifyArrangementId(validArrangementId.value).url)
        val result = route(application, request).value
        status(result) mustEqual NO_CONTENT
      }

      "return 404 for arrangement id in the correct format that does not exist" in {

        when(mockIdService.verifyArrangementId(any())).thenReturn(Future.successful(Some(false)))
        val request =
          FakeRequest(GET, routes.IdController.verifyArrangementId(validArrangementId.value).url)
        val result = route(application, request).value
        status(result) mustEqual NOT_FOUND
      }

      "return 400 for an invalid arrangement id" in {

        when(mockIdService.verifyArrangementId(any())).thenReturn(Future.successful(None))
        val request =
          FakeRequest(GET, routes.IdController.verifyArrangementId("invalid-id").url)
        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }

    }

    "verifyDisclosureIDs" - {
      "return 204 for valid arrangement and disclosure ids and if they are from the same submission" in {

        when(mockIdService.verifyIDs(any(), any(), any())).thenReturn(Future.successful(IdsCorrect))

        val request =
          FakeRequest(GET, routes.IdController.verifyDisclosureIDs(validArrangementId.value, validDisclosureId.value, enrolmentID).url)

        val result = route(application, request).value
        status(result) mustEqual NO_CONTENT
      }

      "return 404 for ids that aren't from the submission" in {

        when(mockIdService.verifyIDs(any(), any(), any())).thenReturn(Future.successful(IdsInDifferentSubmissions))

        val request =
          FakeRequest(GET, routes.IdController.verifyDisclosureIDs(validArrangementId.value, validDisclosureId.value, enrolmentID).url)

        val result = route(application, request).value
        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustEqual "Arrangement ID and Disclosure ID are not from the same submission"
      }

      "return 404 for invalid/missing arrangement id" in {

        when(mockIdService.verifyIDs(any(), any(), any())).thenReturn(Future.successful(ArrangementIDNotFound))

        val request =
          FakeRequest(GET, routes.IdController.verifyDisclosureIDs(validArrangementId.value, validDisclosureId.value, enrolmentID).url)

        val result = route(application, request).value
        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustEqual "Arrangement ID not found"
      }

      "return 404 for disclosure id that doesn't match the enrolment id in the submission" in {

        when(mockIdService.verifyIDs(any(), any(), any())).thenReturn(Future.successful(DisclosureIDNotFound))

        val request =
          FakeRequest(GET, routes.IdController.verifyDisclosureIDs(validArrangementId.value, validDisclosureId.value, enrolmentID).url)

        val result = route(application, request).value
        status(result) mustEqual NOT_FOUND
        contentAsString(result) mustEqual "Disclosure ID doesn't match enrolment ID"
      }

      "return 400 for ids that do not exist or verification failed" in {

        when(mockIdService.verifyIDs(any(), any(), any())).thenReturn(Future.successful(IdsNotFound))

        val request =
          FakeRequest(GET, routes.IdController.verifyDisclosureIDs(validArrangementId.value, validDisclosureId.value, enrolmentID).url)

        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual "IDs not found"
      }

    }

  }

}
