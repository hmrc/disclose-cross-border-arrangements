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
import models.{ArrangementId, DisclosureId, GeneratedIDs}
import org.scalatest.StreamlinedXmlEquality

class TransformServiceSpec extends SpecBase with StreamlinedXmlEquality {

  "TransformationService" - {
    "when given no ids to add must return the same file" in {
      val service = app.injector.instanceOf[TransformService]

      val submissionFile =
        <test>
          <value>Must be Preserved</value>
        </test>

      service.transformFileForIDs(submissionFile, GeneratedIDs(None, None)) mustBe submissionFile
    }

    "when given a disclosure ID to add must return amended file" in {
      val service = app.injector.instanceOf[TransformService]

      val submissionFile =
        <DAC6_Arrangement version="First">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <ArrangementID>GBA20200601AAA000</ArrangementID>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
            <Disclosing></Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val expectedFile =
        <DAC6_Arrangement version="First">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <ArrangementID>GBA20200601AAA000</ArrangementID>
          <DAC6Disclosures>
            <DisclosureID>GBD20200601AAA001</DisclosureID>
            <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
            <Disclosing></Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      service.transformFileForIDs(
        submissionFile,
        GeneratedIDs(None, Some(DisclosureId("GBD", "20200601", "AAA001")))
      ) mustEqual expectedFile
    }

    "when given both arrangementID and disclosure ID to add must return amended file" in {
      val service = app.injector.instanceOf[TransformService]

      val submissionFile =
        <DAC6_Arrangement version="First">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing></Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val expectedFile =
        <DAC6_Arrangement version="First">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <ArrangementID>GBA20200601AAA000</ArrangementID>
          <DAC6Disclosures>
            <DisclosureID>GBD20200701AAA001</DisclosureID>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing></Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      service.transformFileForIDs(
        submissionFile,
        GeneratedIDs(Some(ArrangementId("GBA", "20200601", "AAA000")), Some(DisclosureId("GBD", "20200701", "AAA001")))
      ) mustEqual expectedFile
    }

    "when given both arrangementID and disclosure ID to add without dac6Disclosure must return amended file" in {
      val service = app.injector.instanceOf[TransformService]

      val submissionFile =
        <DAC6_Arrangement version="First">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
        </DAC6_Arrangement>

      val expectedFile =
        <DAC6_Arrangement version="First">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <ArrangementID>GBA20200601AAA000</ArrangementID>
        </DAC6_Arrangement>

      service.transformFileForIDs(
        submissionFile,
        GeneratedIDs(Some(ArrangementId("GBA", "20200601", "AAA000")), Some(DisclosureId("GBD", "20200701", "AAA001")))
      ) mustEqual expectedFile
    }
  }


}
