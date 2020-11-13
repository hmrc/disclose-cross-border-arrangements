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

import java.time.LocalDateTime
import java.util.UUID

import base.SpecBase
import connectors.SubmissionConnector
import helpers.SubmissionFixtures.{minimalPassing, oneError}
import helpers.{ContactFixtures, DateHelper}
import models.{DisclosureId, GeneratedIDs, SubmissionDetails, SubmissionMetaData}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import repositories.SubmissionDetailsRepository
import services.{ContactService, SubmissionService, TransformService}
import uk.gov.hmrc.http.{HeaderNames, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase
  with MockitoSugar
  with BeforeAndAfterEach {

  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val mockDateHelper: DateHelper = mock[DateHelper]
  val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]
  val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  val mockContactService: ContactService = mock[ContactService]

  override def beforeEach(): Unit = {
    reset(mockSubmissionService, mockSubmissionDetailsRepository, mockSubmissionConnector, mockSubmissionConnector)
  }

  "submission controller" - {

    val testDateTime = LocalDateTime.of(2020, 5, 14, 17, 10, 0)
    val submissionDetails: SubmissionDetails = SubmissionDetails(
      enrolmentID = "enrolmentID",
      submissionTime = testDateTime,
      fileName = "my-file.xml",
      arrangementID = Some("GBA20200601AAA000"),
      disclosureID = Some("GBD20200601AAA000"),
      importInstruction = "Add",
      initialDisclosureMA = false)

    val application = applicationBuilder()
      .overrides(
        bind[SubmissionService].toInstance(mockSubmissionService),
        bind[DateHelper].toInstance(mockDateHelper),
        bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository),
        bind[SubmissionConnector].toInstance(mockSubmissionConnector),
        bind[ContactService].toInstance(mockContactService)
      )
      .build()

    "when a file is posted we transform it, send it to the HOD and return OK" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockSubmissionDetailsRepository.storeSubmissionDetails(any()))
        .thenReturn(Future.successful(true))
      when(mockContactService.getLatestContacts(any())(any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val submission = minimalPassing

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe OK
      verify(mockSubmissionConnector, times(1)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(1)).storeSubmissionDetails(Matchers.eq(submissionDetails))
    }

    "when a file is posted we try to get the contacts and there is an error and we respond with InternalServerError" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any()))
        .thenReturn(Future.failed(new Exception("Failed to retrieve and convert subscription")))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
      val submission = minimalPassing

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockSubmissionConnector, times(0)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(0)).storeSubmissionDetails(Matchers.eq(submissionDetails))
    }

    "when a file is posted we try to validate it and there is an error and we respond with InternalServerError" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
      val submission = oneError

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockSubmissionConnector, times(0)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(0)).storeSubmissionDetails(Matchers.eq(submissionDetails))
    }

    "when a file is posted we try to submit it and there is an error we respond with InternalServerError" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
      val submission = minimalPassing

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockSubmissionConnector, times(1)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(0)).storeSubmissionDetails(Matchers.eq(submissionDetails))
    }

    "when a file is posted, send it to the HOD but repository fails to store we return a server error" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockSubmissionDetailsRepository.storeSubmissionDetails(any()))
        .thenReturn(Future.successful(false))
      when(mockContactService.getLatestContacts(any())(any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val submission = minimalPassing

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockSubmissionConnector, times(1)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(1)).storeSubmissionDetails(Matchers.eq(submissionDetails))
    }

    "conversationID is the correct length per spec when passed from frontend" in {
      val mockTransformService = mock[TransformService]
      val app = new GuiceApplicationBuilder()
        .overrides(
          bind[SubmissionService].toInstance(mockSubmissionService),
          bind[DateHelper].toInstance(mockDateHelper),
          bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository),
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ContactService].toInstance(mockContactService),
          bind[TransformService].toInstance(mockTransformService)
        )
        .build()

      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))

      val submission = minimalPassing
      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
        .withHeaders((HeaderNames.xSessionId, s"session-${UUID.randomUUID()}"))
      val result: Future[Result] = route(app, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR

      val argumentCaptorData: ArgumentCaptor[SubmissionMetaData] = ArgumentCaptor.forClass(classOf[SubmissionMetaData])
      verify(mockTransformService, times(1)).addSubscriptionDetailsToSubmission(any(), any(), argumentCaptorData.capture())

      val submissionMetaData = argumentCaptorData.getValue
      val conversationIDLength = submissionMetaData.conversationID.length
      conversationIDLength >= 1 && conversationIDLength <= 36 mustBe true
    }

    "conversationID is the correct length per spec when generated" in {
      val mockTransformService = mock[TransformService]
      val app = new GuiceApplicationBuilder()
        .overrides(
          bind[SubmissionService].toInstance(mockSubmissionService),
          bind[DateHelper].toInstance(mockDateHelper),
          bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository),
          bind[SubmissionConnector].toInstance(mockSubmissionConnector),
          bind[ContactService].toInstance(mockContactService),
          bind[TransformService].toInstance(mockTransformService)
        )
        .build()

      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))

      val submission = minimalPassing
      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR

      val argumentCaptorData: ArgumentCaptor[SubmissionMetaData] = ArgumentCaptor.forClass(classOf[SubmissionMetaData])
      verify(mockTransformService, times(1)).addSubscriptionDetailsToSubmission(any(), any(), argumentCaptorData.capture())

      val submissionMetaData = argumentCaptorData.getValue
      val conversationIDLength = submissionMetaData.conversationID.length
      conversationIDLength >= 1 && conversationIDLength <= 36 mustBe true
    }
  }
}