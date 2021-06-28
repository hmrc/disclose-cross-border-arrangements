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

package services

import base.SpecBase
import helpers.{ErrorMessageHelper, SuffixHelper}
import models.{Dac6MetaData, SubmissionDetails, Validation}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import repositories.SubmissionDetailsRepository

import java.time.LocalDateTime
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class MetaDataValidationServiceSpec extends SpecBase with BeforeAndAfterEach {

  val arrangementId1 = "GBA20200101AAA123"
  val arrangementId2 = "GBA20200101BBB456"

  val disclosureId1 = "GBD20200101AAA123"
  val disclosureId2 = "GBD20200101BBB456"

  val mockSuffixHelper = mock[SuffixHelper]
  val mockSubmissionDetailsRepository = mock[SubmissionDetailsRepository]
  val mockIDService = mock[IdService]
  val mockErrorMessageHelper = mock[ErrorMessageHelper]

  val service = new MetaDataValidationService(mockSuffixHelper, mockSubmissionDetailsRepository, mockIDService, mockErrorMessageHelper)

  implicit val postFixOps = scala.language.postfixOps
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val enrolmentId = "XADAC0001234567"

  val messageRefId1 = s"GB${enrolmentId}XYZ123"
  val messageRefId2 = s"GB${enrolmentId}XYZ789"


  val submissionDateTime1 = LocalDateTime.now()
  val submissionDateTime2 = submissionDateTime1.plusDays(1)
  val submissionDateTime3 = submissionDateTime1.plusDays(2)

  override def beforeEach {
    Mockito.reset(mockSuffixHelper, mockSubmissionDetailsRepository, mockIDService)
  }

  "MetaDataValidationService" -{
    "verifyMetaData" - {
      "should return a ValidationSuccess if ArrangementId matches hmrcs record for DAC6ADD if they are filing under another users arrangemntid" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(List()))
        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
        verify(mockIDService, times(1)).verifyArrangementId(any())

      }

      "should return a ValidationSuccess if ArrangementId matches hmrcs record for DAC6ADD if they are filing under their own arrangemntid" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails)

        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))
        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
        verify(mockIDService, times(0)).verifyArrangementId(any())


      }

      "should return a ValidationFailure if ArrangementId matches hmrcs record for DAC6ADD" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(List()))

        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(false)))
        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords", false))
        verify(mockIDService, times(1)).verifyArrangementId(any())
      }

      "should return a ValidationSuccess if disclosureId relates to them for DAC6REP" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()

      }

      "should return a ValidationFailure if disclosureId does not relate to them for DAC6REP" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchUser", false))

      }


      "should return a ValidationFailure if disclosureId does not relate to them for DAC6DEL" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6DEL", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2),
          importInstruction = "DAC6DEL",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchUser", false))

      }

      "should return a ValidationFailure if disclosureId does not relate to the given ArrangementId" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionDetails2 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime2,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId2),
          disclosureID = Some(disclosureId2),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails1, submissionDetails2)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchArrangementID", false))

      }

      "should return a ValidationSuccess if DAC6ADD for a marketable arrangement has implementingDates populated for new RelevantTaxpayers" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)


        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationSucces if DAC6ADD for an arrangment which is no longer a marketable arrangement and does not have implementingDates populated for new RelevantTaxpayers" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6NEW",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)

        val submissionDetails2 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime2,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)


        val submissionHistory = List(submissionDetails1, submissionDetails2)
        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationSucces if DAC6ADD for an arrangment which is no longer a marketable arrangement and does not have implementingDates " +
        "populated for new RelevantTaxpayers 2" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6NEW",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)

        val submissionDetails2 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime2,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)

        val submissionDetails3 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime3,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)


        val submissionHistory = List(submissionDetails1, submissionDetails2, submissionDetails3)
        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }


      "should return a ValidationFailure if metaData notDefined" in {

        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(List()))
        val result = Await.result(service.verifyMetaData(None, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("File does not contain necessary data", false))
      }


      "should return a ValidationSucces if DAC6NEW has disclosure information" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6NEW", disclosureInformationPresent = true,
          initialDisclosureMA = false, messageRefId = messageRefId1))


        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(List()))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationFailure if DAC6NEW does not have disclosure information" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6NEW", disclosureInformationPresent = false,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(List()))
        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6NEW", false))
      }

      "should return a ValidationSucces for a DAC6ADD when InitialDisclosureMA was false in the DAC6NEW for the ArrangementID" +
        " and disclosure info is present" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1)
          , disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "DAC6REP",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)


        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationSucces for a DAC6ADD when InitialDisclosureMA was true in the DAC6NEW for the ArrangementID" +
        " and disclosure info is present" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1)
          , disclosureInformationPresent = false, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)


        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationFailure for a DAC6ADD when InitialDisclosureMA was false in the DAC6NEW for the ArrangementID" +
        " and disclosure info is not present" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1)
          , disclosureInformationPresent = false, initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)


        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))


        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6ADD", false))
      }

      "should return a ValidationSucces for a DAC6REP which is replacing a DAC6NEW and disclosure info is present" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = true, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationFailure for a DAC6REP which is replacing a DAC6NEW and disclosure info is not present" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = false,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.disclosureInformation.noInfoWhenReplacingDAC6NEW", false))
      }

      "should return a ValidationSucces for a DAC6REP which is replacing a DAC6ADD linked to a non-marketable arrangement and disclsoure info is present" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), disclosureInformationPresent = true,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionDetails2 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2),
          importInstruction = "Add",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails1, submissionDetails2)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationFailure for a DAC6REP which is replacing a DAC6ADD linked to a non-marketable" +
        " arrangement and disclsoure info is not present" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2), disclosureInformationPresent = false,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionDetails2 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId2),
          importInstruction = "Add",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails1, submissionDetails2)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.disclosureInformation.noInfoForNonMaDAC6REP", false))
      }

      "should return a ValidationSuccess for a DAC6REP which is replacing a DAC6NEW and marketable arrangment flag matches" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = true, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationFailure for a DAC6REP which is replacing a DAC6NEW and marketable arrangement flag does not match 1" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = true, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = false,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.initialDisclosureMA.arrangementNowMarketable", false))
      }

      "should return a ValidationFailure for a DAC6REP which is replacing a DAC6NEW and marketable arrangement flag does not match 2" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId2)

        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.initialDisclosureMA.arrangementNoLongerMarketable", false))
      }

      "should return a ValidationSuccess for a file with MessageRefId in correct format" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6NEW", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = true, messageRefId = messageRefId1))

        val submissionHistory = List()
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationSuccess for a file with blank messageRefId (as this will be covered by xsd error)" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6NEW", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = true, messageRefId = ""))

        val submissionHistory = List()
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq()
      }

      "should return a ValidationFailure for a file with MessageRefId which does not start GB" in {

        val invalidMessageRefId = "123456XYZ789"

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6NEW", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = true, messageRefId = invalidMessageRefId))

        val submissionHistory = List()
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.messageRefId.wrongFormat", false))
      }

      "should return a ValidationFailure for a file with MessageRefId which does not contain users enrolment id " +
        "after GB" in {

        val wrongEnrolmentId = "XADAC0007654321"

        val invalidMessageRefId = s"GB${wrongEnrolmentId}XYZ789"

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6NEW", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = true, messageRefId = invalidMessageRefId))

        val submissionHistory = List()
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.messageRefId.noUserId", false))
      }

      "should return a ValidationFailure for a file with MessageRefId which does have any chars after userId" in {


        val invalidMessageRefId = s"GB$enrolmentId"

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6NEW", arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1), disclosureInformationPresent = true,
          initialDisclosureMA = true, messageRefId = invalidMessageRefId))

        val submissionHistory = List()
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.messageRefId.wrongFormat", false))
      }

      "should return a ValidationFailure for a file with MessageRefId has been used before by this user for a DAC6NEW" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6NEW", arrangementID = Some(arrangementId2),
          disclosureInformationPresent = true, initialDisclosureMA = true, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId1)

        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.messageRefId.notUnique", false))
      }

      "should return a ValidationFailure for a file with MessageRefId has been used before by this user for a DAC6ADD" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true, initialDisclosureMA = true, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId1)

        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.messageRefId.notUnique", false))
      }

      "should return a ValidationFailure for a file with MessageRefId has been used before by this user for a DAC6REP" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true, initialDisclosureMA = true, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId1)

        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.messageRefId.notUnique", false))
      }

      "should return a ValidationFailure for a file with MessageRefId has been used before by this user for a DAC6DEL" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6REP", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true, initialDisclosureMA = true, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId1)

        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaData(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Seq(Validation("metaDataRules.messageRefId.notUnique", false))
      }


      "must return MessageRefId for file that passes meta data validation" in {

        val generatedSuffix = "123456"

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        when(mockSuffixHelper.generateSuffix(any())).thenReturn(generatedSuffix)
        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(List()))

        val result = Await.result(service.verifyMetaDataForManualSubmission(dac6MetaData, enrolmentId), 10 seconds)

        result mustBe Right(messageRefId1 + generatedSuffix)

        verify(mockIDService, times(1)).verifyArrangementId(any())

      }

      "must call suffix generator multiple times until unique messageRefId is generated" in {

        val firstGeneratedSuffix = "123456"
        val secondGeneratedSuffix = "654321"

        when(mockSuffixHelper.generateSuffix(any())).thenReturn(
          firstGeneratedSuffix, secondGeneratedSuffix
        )

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true,
          initialDisclosureMA = false, messageRefId = messageRefId1))

        val submissionDetails1 = SubmissionDetails(enrolmentID = enrolmentId,
          submissionTime = submissionDateTime1,
          fileName = "fileName.xml",
          arrangementID = Some(arrangementId1),
          disclosureID = Some(disclosureId1),
          importInstruction = "New",
          initialDisclosureMA = true,
          messageRefId = messageRefId1 + firstGeneratedSuffix)

        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(true)))
        val submissionHistory = List(submissionDetails1)
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(submissionHistory))

        val result = Await.result(service.verifyMetaDataForManualSubmission(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Right(messageRefId1 + secondGeneratedSuffix)
      }

      "must errors for file that fails meta data validation" in {

        val dac6MetaData = Some(Dac6MetaData(importInstruction = "DAC6ADD", arrangementID = Some(arrangementId1),
          disclosureInformationPresent = true, initialDisclosureMA = false, messageRefId = messageRefId1))
        when(mockSubmissionDetailsRepository.retrieveSubmissionHistory(any())).thenReturn(Future.successful(List()))

        when(mockIDService.verifyArrangementId(any())).thenReturn(Future.successful(Some(false)))

        val result = Await.result(service.verifyMetaDataForManualSubmission(dac6MetaData, enrolmentId), 10 seconds)
        result mustBe Left(Seq("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords"))
        verify(mockIDService, times(1)).verifyArrangementId(any())

      }
    }
  }
}
