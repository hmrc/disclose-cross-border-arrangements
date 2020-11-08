/*
 * Copyright 2020 HM Revenue & Customs
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
import generators.ModelGenerators
import models.DisplaySubscriptionForDACRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, route, status, _}
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class SubscriptionControllerSpec extends SpecBase
  with MockitoSugar
  with ModelGenerators
  with ScalaCheckPropertyChecks {

  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  val application: Application = applicationBuilder()
    .overrides(
      bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
    ).build()

  "SubscriptionController" - {
    "displaySubscriptionDetails" - {
      "must return OK and the subscription details to display in frontend" in {

        forAll(arbitrary[DisplaySubscriptionForDACRequest]) {
          displaySubscriptionForDACRequest =>
            when(mockSubscriptionConnector.displaySubscriptionForDAC(any())(any(), any()))
              .thenReturn(Future.successful(HttpResponse(OK, "")))

            val request = FakeRequest(POST, routes.SubscriptionController.displaySubscriptionDetails().url)
              .withJsonBody(Json.toJson(displaySubscriptionForDACRequest))

            val result = route(application, request).value
            status(result) mustEqual OK
        }
      }

      "must return non-OK if EIS returns an error status code (400, 403, 404, 405, 409, 500, 503)" in {

        forAll(arbitrary[DisplaySubscriptionForDACRequest],
          Gen.oneOf(Seq(BAD_REQUEST, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE))) {
            (displaySubscriptionForDACRequest, statusCode) =>
              when(mockSubscriptionConnector.displaySubscriptionForDAC(any())(any(), any()))
                .thenReturn(Future.successful(HttpResponse(statusCode, "")))

              val request = FakeRequest(POST, routes.SubscriptionController.displaySubscriptionDetails().url)
                .withJsonBody(Json.toJson(displaySubscriptionForDACRequest))

              val result = route(application, request).value
              status(result) mustEqual statusCode
        }
      }
    }
  }

}