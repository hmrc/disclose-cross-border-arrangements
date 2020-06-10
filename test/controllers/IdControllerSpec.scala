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

import uk.gov.hmrc.disclosecrossborderarrangements.controllers.routes
import uk.gov.hmrc.disclosecrossborderarrangements.services.IdService
import base.SpecBase
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, status, _}
import play.api.{Application, Configuration}
import uk.gov.hmrc.disclosecrossborderarrangements.models.ArrangementId

import scala.concurrent.Future

class IdControllerSpec extends SpecBase
with ScalaCheckPropertyChecks {


  val mockIdService: IdService = mock[IdService]
  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false"))
    .overrides(
      bind[IdService].toInstance(mockIdService)
    ).build()

  val validArrangementId: ArrangementId = ArrangementId(dateString = "20200601", suffix = "A1B1C1")

  "IdController"-{
    "verifyArrangementId" -{
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


  }


}
