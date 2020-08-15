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
import generators.ModelGenerators
import models.{SubmissionDetails, SubmissionHistory}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, route, running, status}
import repositories.SubmissionDetailsRepository
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.test.Helpers._

import scala.concurrent.Future

class HistoryControllerSpec extends SpecBase
  with ScalaCheckPropertyChecks with ModelGenerators {

  "submissionDetails" - {
    "must return ok with submission details" in {
      val mockSubmissionDetailsRepository = mock[SubmissionDetailsRepository]

      val application =
        applicationBuilder
          .overrides(bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository))
          .build()

      running(application) {
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
    }

    "must return Internal Service Error when there is a problem fetching results" in {
      val mockSubmissionDetailsRepository = mock[SubmissionDetailsRepository]

      val application =
        applicationBuilder
          .overrides(bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository))
          .build()

      running(application) {
         when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any()))
              .thenReturn(Future.failed(new Exception))

            val request = FakeRequest(GET, routes.HistoryController.submissionDetails("123").url)

            val result = route(application, request).value

            status(result) mustEqual INTERNAL_SERVER_ERROR
        }
    }
  }

}
