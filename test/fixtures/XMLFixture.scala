/*
 * Copyright 2022 HM Revenue & Customs
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

package fixtures

object XMLFixture {

  val dac6NotInitialDisclosureMA =
    <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
      <Header>
        <MessageRefId>GB0000000XXX</MessageRefId>
        <Timestamp>2020-05-14T17:10:00</Timestamp>
      </Header>
      <DAC6Disclosures>
        <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        <InitialDisclosureMA>false</InitialDisclosureMA>
      </DAC6Disclosures>
    </DAC6_Arrangement>

  val dac6InitialDisclosureMANotSet =
    <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
      <Header>
        <MessageRefId>GB0000000XXX</MessageRefId>
        <Timestamp>2020-05-14T17:10:00</Timestamp>
      </Header>
      <DAC6Disclosures>
        <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
      </DAC6Disclosures>
    </DAC6_Arrangement>

  val dac6RelevantTaxPayers =
    <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
      <Header>
        <MessageRefId>GB0000000XXX</MessageRefId>
        <Timestamp>2020-05-14T17:10:00</Timestamp>
      </Header>
      <DAC6Disclosures>
        <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        <RelevantTaxPayers>
          <RelevantTaxpayer></RelevantTaxpayer>
          <RelevantTaxpayer></RelevantTaxpayer>
        </RelevantTaxPayers>
      </DAC6Disclosures>
    </DAC6_Arrangement>
}
