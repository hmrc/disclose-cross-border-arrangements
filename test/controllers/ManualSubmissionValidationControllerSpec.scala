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
import controllers.auth.{FakeIdentifierAuthAction, IdentifierAuthAction}
import models.{ManualSubmissionValidationFailure, ManualSubmissionValidationSuccess}
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import services.ManualSubmissionValidationEngine

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.xml.Elem

class ManualSubmissionValidationControllerSpec extends SpecBase with BeforeAndAfterEach {

  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  override def beforeEach(): Unit =
    reset()

  val xml: Elem = <dummyTag></dummyTag>

  val mockValidationEngine: ManualSubmissionValidationEngine = mock[ManualSubmissionValidationEngine]

  val application: Application = applicationBuilder()
    .overrides(
      bind[ManualSubmissionValidationEngine].toInstance(mockValidationEngine),
      bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
    )
    .build()

  "manualSubmissionValidationController" - {

    "return ok with ManualSubmissionValidationSuccess when passes validation" in {

      when(mockValidationEngine.validateManualSubmission(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(ManualSubmissionValidationSuccess("id"))))
      val request                = FakeRequest(POST, routes.ManualSubmissionValidationController.validateManualSubmission.url).withXmlBody(xml)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe 200
      contentAsJson(result) mustBe Json.toJson(ManualSubmissionValidationSuccess("id"))
    }

    "return ok with ManualSubmissionValidationFailure when fails business rule validation" in {

      when(mockValidationEngine.validateManualSubmission(any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(ManualSubmissionValidationFailure(Seq("random-error")))))
      val request                = FakeRequest(POST, routes.ManualSubmissionValidationController.validateManualSubmission.url).withXmlBody(xml)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe 200
      contentAsJson(result) mustBe Json.toJson(ManualSubmissionValidationFailure(List("random-error")))
    }

    "return badRequest when xml parsing fails" in {

      when(mockValidationEngine.validateManualSubmission(any(), any())(any(), any())).thenReturn(Future.successful(None))
      val request                = FakeRequest(POST, routes.ManualSubmissionValidationController.validateManualSubmission.url).withXmlBody(xml)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe 400
    }

  }

}
