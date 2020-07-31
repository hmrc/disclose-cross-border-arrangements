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
import helpers.DateHelper
import models.{ArrangementId, DisclosureId, GeneratedIDs, SubmissionDetails}
import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, Matchers}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import repositories.SubmissionDetailsRepository
import services.{GridFSStorageService, SubmissionService}

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase
  with MockitoSugar
  with BeforeAndAfterEach {

  val mockStorageService: GridFSStorageService = mock[GridFSStorageService]
  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val mockDateHelper: DateHelper = mock[DateHelper]
  val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]

  override def beforeEach(): Unit = {
    reset(mockStorageService, mockSubmissionService, mockSubmissionDetailsRepository)
  }

  "submission controller" - {

    val testDateTime = new DateTime(2020,5,14,17,10,0)
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
        bind[GridFSStorageService].toInstance(mockStorageService),
        bind[SubmissionService].toInstance(mockSubmissionService),
        bind[DateHelper].toInstance(mockDateHelper),
        bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository)
      )
      .build()

    "when a file is posted we store it and send an OK" in {
      when(mockStorageService.writeFileToGridFS(any[String](), any()))
        .thenReturn(Future.successful(true))
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD","20200601","AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockSubmissionDetailsRepository.storeSubmissionDetails(submissionDetails))
        .thenReturn(Future.successful(true))

      val submission =
        <submission>
          <fileName>my-file.xml</fileName>
          <enrolmentID>enrolmentID</enrolmentID>
          <file>
            <DAC6_Arrangement version="First">
              <Header>
                <MessageRefId>GB0000000XXX</MessageRefId>
                <Timestamp>2020-05-14T17:10:00</Timestamp>
              </Header>
              <ArrangementID>GBA20200601AAA000</ArrangementID>
              <DAC6Disclosures>
                <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
                <Disclosing></Disclosing>
                <InitialDisclosureMA>false</InitialDisclosureMA>
              </DAC6Disclosures>
            </DAC6_Arrangement>
          </file>
        </submission>

      val request = FakeRequest(POST, routes.SubmissionController.storeSubmission().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe OK
      verify(mockSubmissionDetailsRepository, times(1)).storeSubmissionDetails(Matchers.eq(submissionDetails))
    }

    "when a file is posted we try to store it and there is an error we respond with InternalServerError" in {
      when(mockStorageService.writeFileToGridFS(any[String](), any()))
        .thenReturn(Future.failed(new Exception("Boom!")))
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD","20200601","AAA000")))))
      when(mockDateHelper.now).thenReturn(testDateTime)
      val submission =
        <submission>
          <fileName>my-file.xml</fileName>
          <enrolmentID>enrolmentID</enrolmentID>
          <file>
            <DAC6_Arrangement version="First">
              <Header>
                <MessageRefId>GB0000000XXX</MessageRefId>
                <Timestamp>2020-05-14T17:10:00</Timestamp>
              </Header>
              <ArrangementID>GBA20200601AAA000</ArrangementID>
              <DAC6Disclosures>
                <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
                <Disclosing></Disclosing>
                <InitialDisclosureMA>false</InitialDisclosureMA>
              </DAC6Disclosures>
            </DAC6_Arrangement>
          </file>
        </submission>

      val request = FakeRequest(POST, routes.SubmissionController.storeSubmission().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe INTERNAL_SERVER_ERROR
    }

   "read a file out of the submission store" in {
     val testXml =
       <test>
         <value>read this</value>
       </test>
     val testBytes = testXml.mkString.getBytes

     when(mockStorageService.readFileFromGridFS(any()))
       .thenReturn(Future.successful(Some(testBytes)))

     //NB: This does not have a route attached as it is for testing only at this point
     val controller = application.injector.instanceOf[SubmissionController]
     val result = controller.readSubmissionFromStore("my-test-file.xml")(FakeRequest("GET", "/"))

     status(result) mustBe OK
     contentAsString(result) mustBe testXml.toString
   }

    "when a file is posted we store it with an altered filename" in {
      val details = submissionDetails.copy(importInstruction = "New", initialDisclosureMA = true)

      when(mockStorageService.writeFileToGridFS(any[String](), any()))
        .thenReturn(Future.successful(true))
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(
          Future.successful(
            GeneratedIDs(Some(ArrangementId("GBA", "20200601", "AAA000")), Some(DisclosureId("GBD","20200601","AAA000")))
          )
        )
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockSubmissionDetailsRepository.storeSubmissionDetails(details)).thenReturn(Future.successful(true))

      val submission =
        <submission>
          <fileName>my-file.xml</fileName>
          <enrolmentID>enrolmentID</enrolmentID>
          <file>
            <DAC6_Arrangement version="First">
              <Header>
                <MessageRefId>GB0000000XXX</MessageRefId>
                <Timestamp>2020-05-14T17:10:00</Timestamp>
              </Header>
              <DAC6Disclosures>
                <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
                <Disclosing></Disclosing>
                <InitialDisclosureMA>true</InitialDisclosureMA>
              </DAC6Disclosures>
            </DAC6_Arrangement>
          </file>
        </submission>

      val request = FakeRequest(POST, routes.SubmissionController.storeSubmission().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe OK
      val fileNameCaptor = ArgumentCaptor.forClass(classOf[String])

      verify(mockStorageService, times(1)).writeFileToGridFS(fileNameCaptor.capture(), any())
      verify(mockSubmissionDetailsRepository, times(1)).storeSubmissionDetails(Matchers.eq(details))
      fileNameCaptor.getValue mustBe "my-file.xml-GBD20200601AAA000-20200514171000"
    }

    "when a replacement file is posted we store it with an altered filename" in {
      val details = submissionDetails.copy(importInstruction = "Replace")

      when(mockStorageService.writeFileToGridFS(any[String](), any()))
        .thenReturn(Future.successful(true))
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(
          Future.successful(
            GeneratedIDs(None, None)
          )
        )
      when(mockDateHelper.now).thenReturn(testDateTime)
      when(mockSubmissionDetailsRepository.storeSubmissionDetails(details)).thenReturn(Future.successful(true))

      val submission =
        <submission>
          <fileName>my-file.xml</fileName>
          <enrolmentID>enrolmentID</enrolmentID>
          <file>
            <DAC6_Arrangement version="First">
              <Header>
                <MessageRefId>GB0000000XXX</MessageRefId>
                <Timestamp>2020-05-14T17:10:00</Timestamp>
              </Header>
              <ArrangementID>GBA20200601AAA000</ArrangementID>
              <DAC6Disclosures>
                <DisclosureID>GBD20200601AAA000</DisclosureID>
                <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
                <Disclosing></Disclosing>
                <InitialDisclosureMA>false</InitialDisclosureMA>
              </DAC6Disclosures>
            </DAC6_Arrangement>
          </file>
        </submission>

      val request = FakeRequest(POST, routes.SubmissionController.storeSubmission().url).withXmlBody(submission)
      val result: Future[Result] = route(application, request).value

      status(result) mustBe OK
      val fileNameCaptor = ArgumentCaptor.forClass(classOf[String])

      verify(mockStorageService, times(1)).writeFileToGridFS(fileNameCaptor.capture(), any())
      verify(mockSubmissionDetailsRepository, times(1)).storeSubmissionDetails(details)
      fileNameCaptor.getValue mustBe "my-file.xml-GBD20200601AAA000-20200514171000"
    }
  }

}
