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

import base.SpecBase
import helpers.DateHelper
import models.{DisclosureId, GeneratedIDs, SubmissionDetails}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import repositories.SubmissionDetailsRepository
import services.SubmissionService

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase
  with MockitoSugar
  with BeforeAndAfterEach {

  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val mockDateHelper: DateHelper = mock[DateHelper]
  val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]

  override def beforeEach(): Unit = {
    reset(mockSubmissionService, mockSubmissionDetailsRepository)
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
        bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository)
      )
      .build()

    "when a file is posted we store it and send an OK" in {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
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

      //TODO: Needs a submission to HOD

      status(result) mustBe OK
      verify(mockSubmissionDetailsRepository, times(1)).storeSubmissionDetails(Matchers.eq(submissionDetails))
    }

    "when a file is posted we try to store it and there is an error we respond with InternalServerError" ignore {
      when(mockSubmissionService.generateIDsForInstruction(any()))
        .thenReturn(Future.successful(GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA000")))))
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

      //TODO: This won't currently work as we are not submitting to a HOD

      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }

}
