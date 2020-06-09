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

package services

import base.SpecBase
import org.joda.time.LocalDate
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.mockito.Mockito
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.disclosecrossborderarrangements.helpers.{DateHelper, SuffixHelper}
import uk.gov.hmrc.disclosecrossborderarrangements.models.{ArrangementId, DisclosureId}
import uk.gov.hmrc.disclosecrossborderarrangements.repositories.{ArrangementIdRepository, DisclosureIdRepository}
import uk.gov.hmrc.disclosecrossborderarrangements.services.IdService

import scala.concurrent.Future

class IdServiceSpec extends SpecBase
  with MockitoSugar
  with ScalaCheckPropertyChecks
  with BeforeAndAfterEach{

  val mockDateHelper: DateHelper = mock[DateHelper]
  val mockSuffixHelper: SuffixHelper = mock[SuffixHelper]
  val mockArrangementIdRepository: ArrangementIdRepository = mock[ArrangementIdRepository]
  val mockDisclosureIdRepository: DisclosureIdRepository = mock[DisclosureIdRepository]

  val service = new IdService(mockDateHelper, mockSuffixHelper, mockArrangementIdRepository,
    mockDisclosureIdRepository)
  val testDate = new LocalDate(2020, 6, 1)

  val newSuffix = "A1B2C3"
  when(mockDateHelper.today).thenReturn(testDate)
  when(mockSuffixHelper.generateSuffix()).thenReturn(newSuffix)

  val arrangementIdRegEx = "[A-Z]{2}[A]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"
  val disclosureIdRegEx = "[A-Z]{2}[D]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"

  val expectedDateString = "20200601"

  val newArrangementId: ArrangementId = ArrangementId(dateString = expectedDateString, suffix = newSuffix)
  val newDisclosureId: DisclosureId = DisclosureId(dateString = expectedDateString, suffix = newSuffix)

  override def beforeEach():Unit = Mockito.reset(mockArrangementIdRepository, mockDisclosureIdRepository)

  val arrangementIdPrefix = "GBA"
  val disclosureIdPrefix = "GBD"

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

        val idAsString = "GBA" + expectedDateString + newSuffix
        val formattedArrangementId = ArrangementId(prefix = "GBA",
                                                   dateString = expectedDateString,
                                                   suffix = newSuffix)
        service.verifyArrangementId(idAsString).futureValue mustBe Some(true)

        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(formattedArrangementId)

      }

      "must return false arrangementId if arrangement id is in the correct format but does not exist" in {
        when(mockArrangementIdRepository.doesArrangementIdExist(any())).thenReturn(Future.successful(false))

        val id = "GBA" + expectedDateString + newSuffix
        service.verifyArrangementId(id).futureValue mustBe Some(false)
        verify(mockArrangementIdRepository, times(1)).doesArrangementIdExist(any())

      }

    }
  }
}

