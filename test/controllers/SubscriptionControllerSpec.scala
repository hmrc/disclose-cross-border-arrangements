/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.SubscriptionConnector
import controllers.auth.{AuthAction, FakeAuthAction}
import generators.ModelGenerators
import models.subscription.{DisplaySubscriptionForDACRequest, UpdateSubscriptionForDACRequest}
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, route, status, _}
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class SubscriptionControllerSpec extends SpecBase with ModelGenerators with ScalaCheckPropertyChecks {

  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  val application: Application = applicationBuilder()
    .overrides(
      bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
      bind[AuthAction].to[FakeAuthAction]
    )
    .build()

  val errorStatusCodes: Seq[Int] = Seq(BAD_REQUEST, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE)

  "SubscriptionController" - {
    "displaySubscriptionDetails" - {
      "must return OK and the subscription details to display in frontend" in {

        forAll(arbitrary[DisplaySubscriptionForDACRequest]) {
          displaySubscriptionForDACRequest =>
            when(mockSubscriptionConnector.displaySubscriptionForDAC(any())(any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, "")))

            val request = FakeRequest(POST, routes.SubscriptionController.displaySubscriptionDetails.url)
              .withJsonBody(Json.toJson(displaySubscriptionForDACRequest))

            val result = route(application, request).value
            status(result) mustEqual OK
        }
      }

      "must return non-OK if EIS returns an error status code (400, 403, 404, 405, 409, 500, 503)" in {

        forAll(arbitrary[DisplaySubscriptionForDACRequest], Gen.oneOf(errorStatusCodes)) {
          (displaySubscriptionForDACRequest, statusCode) =>
            when(mockSubscriptionConnector.displaySubscriptionForDAC(any())(any(), any()))
              .thenReturn(Future.successful(HttpResponse(statusCode, "")))

            val request = FakeRequest(POST, routes.SubscriptionController.displaySubscriptionDetails.url)
              .withJsonBody(Json.toJson(displaySubscriptionForDACRequest))

            val result = route(application, request).value
            status(result) mustBe statusCode
        }
      }
    }

    "updateSubscription" - {
      "must return OK if update request is valid and successful" in {

        forAll(arbitrary[UpdateSubscriptionForDACRequest]) {
          updateSubscriptionForDAC =>
            when(mockSubscriptionConnector.updateSubscriptionForDAC(any())(any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, "")))

            val request = FakeRequest(POST, routes.SubscriptionController.updateSubscription.url)
              .withJsonBody(Json.toJson(updateSubscriptionForDAC))

            val result = route(application, request).value
            status(result) mustBe OK
        }
      }

      "must return non-OK if EIS returns an error status code (400, 403, 404, 405, 409, 500, 503)" in {

        forAll(arbitrary[UpdateSubscriptionForDACRequest], Gen.oneOf(errorStatusCodes)) {
          (updateSubscriptionForDAC, statusCode) =>
            when(mockSubscriptionConnector.updateSubscriptionForDAC(any())(any(), any()))
              .thenReturn(Future.successful(HttpResponse(statusCode, "")))

            val request = FakeRequest(POST, routes.SubscriptionController.updateSubscription.url)
              .withJsonBody(Json.toJson(updateSubscriptionForDAC))

            val result = route(application, request).value
            status(result) mustBe statusCode
        }
      }
    }
  }

}
