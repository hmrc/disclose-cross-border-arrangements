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
import connectors.{SubmissionConnector, SubscriptionConnector}
import controllers.auth.{FakeIdentifierAuthAction, IdentifierAuthAction}
import generators.CacheModelGenerators
import helpers.SubmissionFixtures.{minimalPassing, oneError}
import helpers.{ContactFixtures, DateHelper}
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers, MockitoSugar}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import repositories.SubmissionDetailsRepository
import services.{ContactService, SubmissionService, SubscriptionCacheService, TransformService}
import uk.gov.hmrc.http.{HeaderNames, HttpResponse, UpstreamErrorResponse}

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future
import scala.xml.NodeSeq

class SubmissionControllerSpec extends SpecBase
  with MockitoSugar
  with ScalaCheckDrivenPropertyChecks
  with CacheModelGenerators
  with BeforeAndAfterEach {

  import APIDateTimeFormats._

  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val mockDateHelper: DateHelper = mock[DateHelper]
  val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]
  val mockSubmissionConnector: SubmissionConnector = mock[SubmissionConnector]
  val mockContactService: ContactService = mock[ContactService]
  val mockSubscriptionCacheService: SubscriptionCacheService = mock[SubscriptionCacheService]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]


  val errorStatusCodes: Seq[Int] = Seq(BAD_REQUEST, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED,
    CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE)

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
      initialDisclosureMA = false,
      messageRefId = "GB0000000XXX")

    val application = applicationBuilder()
      .overrides(
        bind[SubmissionService].toInstance(mockSubmissionService),
        bind[DateHelper].toInstance(mockDateHelper),
        bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository),
        bind[SubmissionConnector].toInstance(mockSubmissionConnector),
        bind[ContactService].toInstance(mockContactService),
        bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
      )
      .build()

    "when a file is posted we transform it, send it to the HOD and return OK" in {

      val disclosureId = DisclosureId("GBD", "20200601", "AAA000")

      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(disclosureId))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockSubmissionDetailsRepository.storeSubmissionDetails(any()))
        .thenReturn(Future.successful(true))
      when(mockContactService.getLatestContacts(any())(any(), any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val submission = minimalPassing

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe OK

      val argumentCaptor: ArgumentCaptor[NodeSeq] = ArgumentCaptor.forClass(classOf[NodeSeq])

      verify(mockSubmissionConnector, times(1)).submitDisclosure(argumentCaptor.capture())(any())
      verify(mockSubmissionDetailsRepository, times(1)).storeSubmissionDetails(ArgumentMatchers.eq(submissionDetails))

      val xmlWithIds = argumentCaptor.getValue
      val generatedDisclosureId = (xmlWithIds \\ "DisclosureID").text
      generatedDisclosureId mustBe disclosureId.value

    }

    "when a file is posted we try to get the contacts and there is an error and we respond with InternalServerError" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any(), any()))
        .thenReturn(Future.failed(new Exception("Failed to retrieve and convert subscription")))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
      val submission = minimalPassing

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockSubmissionConnector, times(0)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(0)).storeSubmissionDetails(ArgumentMatchers.eq(submissionDetails))
    }

    "when a file is posted we try to validate it and there is an error and we respond with InternalServerError" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
      val submission = oneError

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockSubmissionConnector, times(0)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(0)).storeSubmissionDetails(ArgumentMatchers.eq(submissionDetails))
    }

    "when a file is posted we try to submit it and there is an error we respond with InternalServerError" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
      val submission = minimalPassing

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockSubmissionConnector, times(1)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(0)).storeSubmissionDetails(ArgumentMatchers.eq(submissionDetails))
    }

    "when a file is posted, send it to the HOD but repository fails to store we return a server error" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockSubmissionDetailsRepository.storeSubmissionDetails(any()))
        .thenReturn(Future.successful(false))
      when(mockContactService.getLatestContacts(any())(any(), any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.successful(HttpResponse(OK, "")))

      val submission = minimalPassing

      val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockSubmissionConnector, times(1)).submitDisclosure(any())(any())
      verify(mockSubmissionDetailsRepository, times(1)).storeSubmissionDetails(ArgumentMatchers.eq(submissionDetails))
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
          bind[TransformService].toInstance(mockTransformService),
          bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
        )
        .build()

      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
      when(mockTransformService.addNameSpaces(any(), any())).thenReturn(minimalPassing)

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
          bind[TransformService].toInstance(mockTransformService),
          bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
        )
        .build()

      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockContactService.getLatestContacts(any())(any(), any(), any()))
        .thenReturn(Future.successful(ContactFixtures.contact))
      when(mockSubmissionConnector.submitDisclosure(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("", INTERNAL_SERVER_ERROR)))
      when(mockTransformService.addNameSpaces(any(), any())).thenReturn(minimalPassing)

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

    "must convert successfully when not a 200 response (400, 403, 404, 405, 409, 500, 503)" in {

      val disclosureId = DisclosureId("GBD", "20200601", "AAA000")


      forAll(Gen.oneOf(errorStatusCodes)) {
        statusCode =>

          when(mockSubmissionService.generateIDsForInstruction(any()))
            .thenReturn(Future.successful(GeneratedIDs(None, Some(disclosureId))))
          when(mockDateHelper.now).thenReturn(testDateTime)
          when(mockSubmissionDetailsRepository.storeSubmissionDetails(any()))
            .thenReturn(Future.successful(true))
          when(mockContactService.getLatestContacts(any())(any(), any(), any()))
            .thenReturn(Future.successful(ContactFixtures.contact))
          when(mockSubmissionConnector.submitDisclosure(any())(any()))
            .thenReturn(Future.successful(HttpResponse(statusCode, "")))

        val submission = minimalPassing


        val request = FakeRequest(POST, routes.SubmissionController.submitDisclosure().url).withXmlBody(submission)
        val result: Future[Result] = route(application, request).value

        status(result) mustBe statusCode
      }

    }

    "must return an OK history for getHistory when enrolmentID provided" in {
      val fileName = "fileName"

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

      val submissionDetailsList = List(initialSubmissionDetails)

      when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(fileName))
        .thenReturn(Future.successful(submissionDetailsList))

      val request = FakeRequest(GET, routes.SubmissionController.getHistory(fileName).url)

      val result = route(application, request).value

      status(result) mustEqual OK
      contentAsJson(result) mustEqual Json.toJson(SubmissionHistory(submissionDetailsList))
    }
  }
}