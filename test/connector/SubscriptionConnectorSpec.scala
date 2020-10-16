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

package connector

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import connectors.SubscriptionConnector
import controllers.Assets.OK
import generators.ModelGenerators
import helpers.WireMockServerHandler
import models.DisplaySubscriptionForDACRequest
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{BAD_REQUEST, CONFLICT, FORBIDDEN, INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED, NOT_FOUND, SERVICE_UNAVAILABLE}

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionConnectorSpec extends SpecBase
  with WireMockServerHandler
  with MockitoSugar
  with ModelGenerators
  with ScalaCheckPropertyChecks {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.registration.port" -> server.port()
    ).build()

  lazy val connector: SubscriptionConnector = app.injector.instanceOf[SubscriptionConnector]

  "SubscriptionConnector" - {

    "displaySubscriptionForDAC" - {
      "must return status OK for a successful request to display subscription" in {

        forAll(arbitrary[DisplaySubscriptionForDACRequest]) {
          displaySubscriptionForDACRequest =>
            stubResponse("/register-for-cross-border-arrangement-stubs/dac6/dct04/v1", OK)

            val result = connector.displaySubscriptionForDAC(displaySubscriptionForDACRequest)
            result.futureValue.status mustBe OK
        }
      }

      "must return status non-OK for an unsuccessful request to display subscription" in {

        forAll(arbitrary[DisplaySubscriptionForDACRequest],
          Gen.oneOf(Seq(BAD_REQUEST, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE))) {
            (displaySubscriptionForDACRequest, statusCode) =>
              stubResponse("/register-for-cross-border-arrangement-stubs/dac6/dct04/v1", statusCode)

              val result = connector.displaySubscriptionForDAC(displaySubscriptionForDACRequest)
              result.futureValue.status mustBe statusCode
        }
      }
    }
  }

  private def stubResponse(expectedUrl: String, expectedStatus: Int): StubMapping =
    server.stubFor(
      post(urlEqualTo(expectedUrl))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
        )
    )

}
