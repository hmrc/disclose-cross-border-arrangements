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
import models._
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind

import scala.concurrent.Future

class SubmissionServiceSpec extends SpecBase
  with MockitoSugar
  with BeforeAndAfterEach {

  val mockIDService = mock[IdService]

  val application = applicationBuilder()
    .overrides(
      bind[IdService].toInstance(mockIDService)
    )
    .build()

  override def beforeEach(): Unit = {
    reset(mockIDService)
  }

  "Submission Service" - {
    "when a New instruction is sent it generates a disclosureID and arrangementID" in {
      when(mockIDService.generateDisclosureId())
        .thenReturn(Future.successful(DisclosureId("GBD", "20200601", "AAA001")))

      when(mockIDService.generateArrangementId())
        .thenReturn(Future.successful(ArrangementId("GBA", "20200601", "AAA000")))

      val service = application.injector.instanceOf[SubmissionService]

      val id = service.generateIDsForInstruction(New).futureValue
      verify(mockIDService, times(1)).generateDisclosureId()
      verify(mockIDService, times(1)).generateArrangementId()

      id mustBe GeneratedIDs(Some(ArrangementId("GBA", "20200601", "AAA000")), Some(DisclosureId("GBD", "20200601", "AAA001")))
    }

    "when a Add instruction is sent it generates a disclosureID" in {
      when(mockIDService.generateDisclosureId())
        .thenReturn(Future.successful(DisclosureId("GBD", "20200601", "AAA001")))

      when(mockIDService.generateArrangementId())
        .thenReturn(Future.successful(ArrangementId("GBA", "20200601", "AAA000")))

      val service = application.injector.instanceOf[SubmissionService]

      val id = service.generateIDsForInstruction(Add).futureValue
      verify(mockIDService, times(1)).generateDisclosureId()
      verify(mockIDService, times(0)).generateArrangementId()

      id mustBe GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA001")))
    }

    "when a Replace instruction is sent it doesnt generate any" in {
      when(mockIDService.generateDisclosureId())
        .thenReturn(Future.successful(DisclosureId("GBD", "20200601", "AAA001")))

      when(mockIDService.generateArrangementId())
        .thenReturn(Future.successful(ArrangementId("GBA", "20200601", "AAA000")))

      val service = application.injector.instanceOf[SubmissionService]

      val id = service.generateIDsForInstruction(Replace).futureValue
      verify(mockIDService, times(0)).generateDisclosureId()
      verify(mockIDService, times(0)).generateArrangementId()

      id mustBe GeneratedIDs(None, None)
    }

    "when a Delete instruction is sent it doesnt generate any" in {
      when(mockIDService.generateDisclosureId())
        .thenReturn(Future.successful(DisclosureId("GBD", "20200601", "AAA001")))

      when(mockIDService.generateArrangementId())
        .thenReturn(Future.successful(ArrangementId("GBA", "20200601", "AAA000")))

      val service = application.injector.instanceOf[SubmissionService]

      val id = service.generateIDsForInstruction(Delete).futureValue
      verify(mockIDService, times(0)).generateDisclosureId()
      verify(mockIDService, times(0)).generateArrangementId()

      id mustBe GeneratedIDs(None, None)
    }
  }

}
