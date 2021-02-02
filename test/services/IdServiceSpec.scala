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
import helpers.{DateHelper, SuffixHelper}
import models.{ArrangementId, DisclosureId}
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import repositories.{ArrangementIdRepository, DisclosureIdRepository, SubmissionDetailsRepository}

import java.time.LocalDate
import scala.concurrent.Future

class IdServiceSpec extends SpecBase
  with MockitoSugar
  with ScalaCheckPropertyChecks
  with BeforeAndAfterEach {

  val mockDateHelper: DateHelper = mock[DateHelper]
  val mockSuffixHelper: SuffixHelper = mock[SuffixHelper]
  val mockArrangementIdRepository: ArrangementIdRepository = mock[ArrangementIdRepository]
  val mockDisclosureIdRepository: DisclosureIdRepository = mock[DisclosureIdRepository]
  val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]

  val service = new IdService(mockDateHelper, mockSuffixHelper, mockArrangementIdRepository,
    mockDisclosureIdRepository, mockSubmissionDetailsRepository)
  val testDate: LocalDate = LocalDate.of(2020, 6, 1)
  val enrolmentID: String = "XADAC0001234567"

  val newSuffix = "A1B2C3"
  when(mockDateHelper.today).thenReturn(testDate)
  when(mockSuffixHelper.generateSuffix()).thenReturn(newSuffix)

  val arrangementIdRegEx = "[A-Z]{2}[A]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"
  val disclosureIdRegEx = "[A-Z]{2}[D]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"

  val expectedDateString = "20200601"

  val newArrangementId: ArrangementId = ArrangementId(dateString = expectedDateString, suffix = newSuffix)
  val newDisclosureId: DisclosureId = DisclosureId(dateString = expectedDateString, suffix = newSuffix)

  override def beforeEach(): Unit = {
    reset(mockArrangementIdRepository, mockDisclosureIdRepository, mockSubmissionDetailsRepository)
  }

  val arrangementIdPrefix = "GBA"
  val disclosureIdPrefix = "GBD"

  val arrangementID: String = arrangementIdPrefix + expectedDateString + newSuffix
  val disclosureID: String = disclosureIdPrefix + expectedDateString + newSuffix

  "IdService"- {

    "generateArrangementId" -{

      "must generate an arrangement Id in the correct format and check for uniqueness" +
        "and the store in mongo" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(false))
        when(mockArrangementIdRepository.storeArrangementId(any())).thenReturn(Future.successful(newArrangementId))

        val id = service.generateArrangementId().futureValue
        id.value.matches(arrangementIdRegEx) mustBe true
        id.prefix mustBe arrangementIdPrefix
        id.dateString mustBe expectedDateString
        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(newArrangementId)
        verify(mockArrangementIdRepository, times(1)).storeArrangementId(newArrangementId)
      }

      "must generate an arrangement Id in the correct format and check for uniqueness" +
        "and regenerate another Id if first Id is not unique" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(true)).thenReturn(Future.successful(false))
        when(mockArrangementIdRepository.storeArrangementId(any())).thenReturn(Future.successful(newArrangementId))

        val id = service.generateArrangementId().futureValue
        id.value.matches(arrangementIdRegEx) mustBe true
        id.prefix mustBe arrangementIdPrefix
        id.dateString mustBe expectedDateString
        verify(mockArrangementIdRepository, times(2)).doesArrangementIdExist(any())
        verify(mockArrangementIdRepository, times(1)).storeArrangementId(any())

      }

    }

    "generateDisclosureId" -{
      "must generate a disclosure Id in the correct format and check for uniqueness" +
        "and the store in mongo" in {
        when(mockDisclosureIdRepository.doesDisclosureIdExist(any())).thenReturn(Future.successful(false))
        when(mockDisclosureIdRepository.storeDisclosureId(any())).thenReturn(Future.successful(newDisclosureId))

        val id = service.generateDisclosureId().futureValue
        id.value.matches(disclosureIdRegEx) mustBe true
        id.prefix mustBe disclosureIdPrefix
        id.dateString mustBe expectedDateString
        verify(mockDisclosureIdRepository, times(1)).doesDisclosureIdExist(newDisclosureId)
        verify(mockDisclosureIdRepository, times(1)).storeDisclosureId(newDisclosureId)
      }

      "must generate an disclosure Id in the correct format and check for uniqueness" +
        "and regenerate another Id if first Id is not unique" in {
        when(mockDisclosureIdRepository.doesDisclosureIdExist(any())).thenReturn(Future.successful(true)).thenReturn(Future.successful(false))
        when(mockDisclosureIdRepository.storeDisclosureId(any())).thenReturn(Future.successful(newDisclosureId))

        val id = service.generateDisclosureId().futureValue
        id.value.matches(disclosureIdRegEx) mustBe true
        id.prefix mustBe disclosureIdPrefix
        id.dateString mustBe expectedDateString
        verify(mockDisclosureIdRepository, times(2)).doesDisclosureIdExist(any())
        verify(mockDisclosureIdRepository, times(1)).storeDisclosureId(any())

      }
    }

    "does arrangementId exist" -{
      "must return true if arrangementId is in correct format and exists" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(true))

        val formattedArrangementId = ArrangementId(prefix = "GBA",
                                                   dateString = expectedDateString,
                                                   suffix = newSuffix)
        service.verifyArrangementId(arrangementID).futureValue mustBe Some(true)

        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(formattedArrangementId)

      }

      "must return true for nonuk arrangment id in correct format" in {

        val nonUkCodes = List("AT", "BE", "BG", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "HR", "IE", "IT",
          "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE")

        nonUkCodes.foreach {
          code => {
            val idAsString = s"${code}A" + expectedDateString + newSuffix
            service.verifyArrangementId(idAsString).futureValue mustBe Some(true)

            verify(mockArrangementIdRepository, times(0)).doesArrangementIdExist(any())
          }
        }
      }

      "must false for uk suffix that is not a valid prefix" in {

        val idAsString = s"ZZA" + expectedDateString + newSuffix
        service.verifyArrangementId(idAsString).futureValue mustBe None

        verify(mockArrangementIdRepository, times(0)).doesArrangementIdExist(any())

      }

      "must return false arrangementId if arrangement id is in the correct format but does not exist" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(false))

        service.verifyArrangementId(arrangementID).futureValue mustBe Some(false)
        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(any())

      }

    }

    "does disclosureId exist" - {
      "must return true if disclosureId is in correct format and matches the enrolment id in the submission" in {
        when(mockSubmissionDetailsRepository.doesDisclosureIdMatchEnrolmentID(any(), any())).thenReturn(Future.successful(true))

        val formattedArrangementId = DisclosureId(dateString = expectedDateString, suffix = newSuffix)

        service.verifyDisclosureId(disclosureID, enrolmentID).futureValue mustBe Some(true)

        verify(mockSubmissionDetailsRepository, times(1)).doesDisclosureIdMatchEnrolmentID(formattedArrangementId.value, enrolmentID)
      }

      "must return true for nonuk disclosureId in correct format" in {
        val nonUkCodes = List("AT", "BE", "BG", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "HR", "IE", "IT",
          "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE")

        nonUkCodes.foreach {
          code => {
            val idAsString = s"${code}D" + expectedDateString + newSuffix
            service.verifyDisclosureId(idAsString, enrolmentID).futureValue mustBe Some(true)

            verify(mockSubmissionDetailsRepository, times(0)).doesDisclosureIdMatchEnrolmentID(any(), any())
          }
        }
      }

      "must return false for uk suffix that is not a valid prefix" in {
        val idAsString = "ZZD" + expectedDateString + newSuffix
        service.verifyDisclosureId(idAsString, enrolmentID).futureValue mustBe None

        verify(mockSubmissionDetailsRepository, times(0)).doesDisclosureIdMatchEnrolmentID(any(), any())
      }

      "must return false if disclosureId is in the correct format but does not match enrolment id in the submission" in {
        when(mockSubmissionDetailsRepository.doesDisclosureIdMatchEnrolmentID(any(), any())).thenReturn(Future.successful(false))

        service.verifyDisclosureId(disclosureID, enrolmentID).futureValue mustBe Some(false)
      }

    }

    "verifyIDs" - {
      "must return (Some(true), Some(true)) if arrangement, disclosure and enrolment IDs are from the same submission" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(true))
        when(mockSubmissionDetailsRepository.doesDisclosureIdMatchEnrolmentID(any(), any())).thenReturn(Future.successful(true))
        when(mockSubmissionDetailsRepository.doesDisclosureIdMatchArrangementID(any(), any())).thenReturn(Future.successful(true))

        service.verifyIDs(arrangementID, disclosureID, enrolmentID).futureValue mustBe Tuple2(Some(true), Some(true))

        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(any())
        verify(mockSubmissionDetailsRepository, times(1)).doesDisclosureIdMatchEnrolmentID(any(), any())
        verify(mockSubmissionDetailsRepository, times(1)).doesDisclosureIdMatchArrangementID(any(), any())
      }

      "must return (Some(false), Some(false)) if arrangement and disclosure IDs aren't from the same submission" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(true))
        when(mockSubmissionDetailsRepository.doesDisclosureIdMatchEnrolmentID(any(), any())).thenReturn(Future.successful(true))
        when(mockSubmissionDetailsRepository.doesDisclosureIdMatchArrangementID(any(), any())).thenReturn(Future.successful(false))

        service.verifyIDs(arrangementID, disclosureID, enrolmentID).futureValue mustBe Tuple2(Some(false), Some(false))

        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(any())
        verify(mockSubmissionDetailsRepository, times(1)).doesDisclosureIdMatchEnrolmentID(any(), any())
        verify(mockSubmissionDetailsRepository, times(1)).doesDisclosureIdMatchArrangementID(any(), any())
      }

      "must return (Some(false), Some(true)) if arrangement ID doesn't exist" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(false))
        when(mockSubmissionDetailsRepository.doesDisclosureIdMatchEnrolmentID(any(), any())).thenReturn(Future.successful(true))

        service.verifyIDs(arrangementID, disclosureID, enrolmentID).futureValue mustBe Tuple2(Some(false), Some(true))

        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(any())
        verify(mockSubmissionDetailsRepository, times(1)).doesDisclosureIdMatchEnrolmentID(any(), any())
        verify(mockSubmissionDetailsRepository, times(0)).doesDisclosureIdMatchArrangementID(any(), any())
      }

      "must return (Some(true), Some(false)) if disclosure ID doesn't exist" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(true))
        when(mockSubmissionDetailsRepository.doesDisclosureIdMatchEnrolmentID(any(), any())).thenReturn(Future.successful(false))

        service.verifyIDs(arrangementID, disclosureID, enrolmentID).futureValue mustBe Tuple2(Some(true), Some(false))

        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(any())
        verify(mockSubmissionDetailsRepository, times(1)).doesDisclosureIdMatchEnrolmentID(any(), any())
        verify(mockSubmissionDetailsRepository, times(0)).doesDisclosureIdMatchArrangementID(any(), any())
      }
    }
  }
}

