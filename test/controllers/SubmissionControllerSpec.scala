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
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import services.GridFSStorageService

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase with MockitoSugar {

  "submission controller" - {

    val mockStorageService = mock[GridFSStorageService]

    val application = applicationBuilder()
      .overrides(
        bind[GridFSStorageService].toInstance(mockStorageService)
      )
      .build()

    "when a file is posted we store it and send an OK" in {
      when(mockStorageService.writeFileToGridFS(any[String](), any()))
        .thenReturn(Future.successful(true))

      val submission =
        <submission>
          <fileName>my-file.xml</fileName>
          <file>
            <test>
              <result>Success!</result>
            </test>
          </file>
        </submission>

      val request = FakeRequest(POST, routes.SubmissionController.storeSubmission.url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe OK
    }

    "when a file is posted we try to store it and there is an error we respond with InternalServerError" in {
      when(mockStorageService.writeFileToGridFS(any[String](), any()))
        .thenReturn(Future.failed(new Exception("Boom!")))

      val submission =
        <submission>
           <fileName>my-file.xml</fileName>
           <file>
             <test>
               <result>Success!</result>
             </test>
            </file>
        </submission>

      val request = FakeRequest(POST, routes.SubmissionController.storeSubmission.url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

/*    "when a submission is badly formed we respond with a BadRequest" in {
      when(mockStorageService.writeFileToGridFS(any[String](), any()))
        .thenReturn(Future.failed(new Exception("Boom!")))

      val controller = application.injector.instanceOf[SubmissionController]

      val submission =
        """
          |{
          | "fileName": ""
          |}
          |""".stripMargin


      val result: Future[Result] = controller.storeSubmission()(FakeRequest("POST", "", FakeHeaders(), Json.parse(submission)))

      status(result) mustBe BAD_REQUEST
    }*/
  }

}
