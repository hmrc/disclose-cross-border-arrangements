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
import org.scalatest.StreamlinedXmlEquality

import scala.xml.NodeSeq

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

  "must transform an individual without a middle name" in {
    val service = app.injector.instanceOf[TransformService]
    val individual = IndividualDetails(
      firstName = "firstName",
      middleName = None,
      lastName = "lastName"
    )

    val expected =
      <individualDetails>
        <firstName>firstName</firstName>
        <lastName>lastName</lastName>
      </individualDetails>

    val result = <individualDetails>
      {service.transformIndividual(individual)}
    </individualDetails>

    result mustEqual expected
  }

  "must transform an individual with a middle name" in {
    val service = app.injector.instanceOf[TransformService]
    val individual = IndividualDetails(
      firstName = "firstName",
      middleName = Some("middleName"),
      lastName = "lastName"
    )

    val expected =
      <individualDetails>
        <firstName>firstName</firstName>
        <middleName>middleName</middleName>
        <lastName>lastName</lastName>
      </individualDetails>

    val result = <individualDetails>
      {service.transformIndividual(individual)}
    </individualDetails>

    result mustEqual expected
  }

  "must transform ContactInformation with individual" in {
    val service = app.injector.instanceOf[TransformService]

    val contactInformation = ContactInformationForIndividual(
      email = "aaa",
      phone = Some("bbb"),
      mobile = Some("ccc"),
      individual = IndividualDetails(
        firstName = "firstName",
        middleName = Some("middleName"),
        lastName = "lastName"
      )
    )

    val expected =
      <contactDetails>
        <phoneNumber>bbb</phoneNumber>
        <mobileNumber>ccc</mobileNumber>
        <emailAddress>aaa</emailAddress>
        <individualDetails>
          <firstName>firstName</firstName>
          <middleName>middleName</middleName>
          <lastName>lastName</lastName>
        </individualDetails>
      </contactDetails>

    val result = <contactDetails>
      {service.transformContactInformation(contactInformation)}
    </contactDetails>

    result mustEqual expected
  }

  "must transform ContactInformation with organisation" in {
    val service = app.injector.instanceOf[TransformService]

    val contactInformation = ContactInformationForOrganisation(
      email = "aaa",
      phone = Some("bbb"),
      mobile = None,
      organisation = OrganisationDetails(
        organisationName = "Example"
      )
    )

    val expected =
      <contactDetails>
        <phoneNumber>bbb</phoneNumber>
        <emailAddress>aaa</emailAddress>
        <organisationDetails>
          <organisationName>Example</organisationName>
        </organisationDetails>
      </contactDetails>

    val result = <contactDetails>
      {service.transformContactInformation(contactInformation)}
    </contactDetails>

    result mustEqual expected
  }

  "must transform Subscription Details" ignore {
    val service = app.injector.instanceOf[TransformService]

    val contactInformation =
      SubscriptionDetails(
        subscriptionID = "subscriptionID",
        tradingName = Some("tradingName"),
        isGBUser = true,
        primaryContact = ContactInformationForOrganisation(
          email = "aaa",
          phone = Some("bbb"),
          mobile = None,
          organisation = OrganisationDetails(
            organisationName = "Example"
          )
        ),
        secondaryContact = Some(ContactInformationForOrganisation(
          email = "ddd",
          phone = Some("eee"),
          mobile = Some("fff"),
          organisation = OrganisationDetails(
            organisationName = "AnotherExample"
          )
        ))
      )

    val expected =
      <subscriptionDetails>
        <subscriptionID>subscriptionID</subscriptionID>
        <tradingName>tradingName</tradingName>
        <isGBUser>true</isGBUser>
        <primaryContact>
          <phoneNumber>bbb</phoneNumber>
          <emailAddress>aaa</emailAddress>
          <organisationDetails>
            <organisationName>Example</organisationName>
          </organisationDetails>
        </primaryContact>
        <secondaryContact>
          <phoneNumber>eee</phoneNumber>
          <mobileNumber>fff</mobileNumber>
          <emailAddress>ddd</emailAddress>
          <organisationDetails>
            <organisationName>AnotherExample</organisationName>
          </organisationDetails>
        </secondaryContact>
      </subscriptionDetails>

    val result = <subscriptionDetails>
      {service.transformSubscriptionDetails(contactInformation, None)}
    </subscriptionDetails>

    expected mustEqual result
  }

  "must add Namespaces to a file" in {
    val service = app.injector.instanceOf[TransformService]
    val file =
      <DAC6UKSubmissionInboundRequest xmlns:dac6="urn:eu:taxud:dac6:v1"
                                      xmlns:eis="http://www.hmrc.gov.uk/dac6/eis"
                                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                      xsi:schemaLocation="http://www.hmrc.gov.uk/dac6/eis/DCT06_EIS_UK_schema.xsd">
        <file>
          <result>true</result>
        </file>
      </DAC6UKSubmissionInboundRequest>

    val expected =
      <eis:DAC6UKSubmissionInboundRequest xmlns:dac6="urn:eu:taxud:dac6:v1"
                                          xmlns:eis="http://www.hmrc.gov.uk/dac6/eis"
                                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                          xsi:schemaLocation="http://www.hmrc.gov.uk/dac6/eis/DCT06_EIS_UK_schema.xsd">
        <eis:file>
          <eis:result>true</eis:result>
        </eis:file>
      </eis:DAC6UKSubmissionInboundRequest>

    val result: NodeSeq = service.addNameSpaces(file, Seq(NamespaceForNode("DAC6UKSubmissionInboundRequest", "eis")))

    result mustBe expected
  }

  "must handle two Namespaces in a file" in {
    val service = app.injector.instanceOf[TransformService]
    val file =
      <DAC6UKSubmissionInboundRequest xmlns:dac6="urn:eu:taxud:dac6:v1"
                                      xmlns:eis="http://www.hmrc.gov.uk/dac6/eis"
                                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                      xsi:schemaLocation="http://www.hmrc.gov.uk/dac6/eis/DCT06_EIS_UK_schema.xsd">
        <file>
          <result>true</result>
          <requestDetail>
            <DAC6_Arrangement xmlns:dac6="urn:ukdac6:v0.1"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   version="2020-04-16T09:30:47Z">
              <submission>Submitted Data</submission>
            </DAC6_Arrangement>
          </requestDetail>
        </file>
      </DAC6UKSubmissionInboundRequest>

    val expected =
      <eis:DAC6UKSubmissionInboundRequest xmlns:dac6="urn:eu:taxud:dac6:v1"
                                          xmlns:eis="http://www.hmrc.gov.uk/dac6/eis"
                                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                          xsi:schemaLocation="http://www.hmrc.gov.uk/dac6/eis/DCT06_EIS_UK_schema.xsd">
        <eis:file>
          <eis:result>true</eis:result>
          <eis:requestDetail>
            <dac6:DAC6_Arrangement xmlns:dac6="urn:ukdac6:v0.1"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   version="2020-04-16T09:30:47Z">
              <dac6:submission>Submitted Data</dac6:submission>
            </dac6:DAC6_Arrangement>
          </eis:requestDetail>
        </eis:file>
      </eis:DAC6UKSubmissionInboundRequest>

    val result: NodeSeq = service.addNameSpaces(file, Seq(
      NamespaceForNode("DAC6UKSubmissionInboundRequest", "eis"),
      NamespaceForNode("DAC6_Arrangement", "dac6")
    ))

    result mustBe expected
  }


  "add namespace definitions for dac6 arrangement" in {
    val service = app.injector.instanceOf[TransformService]
    val file = <DAC6_Arrangement version="2020-04-16T09:30:47Z">
      <submission>Submitted Data</submission>
    </DAC6_Arrangement>

    val expected = <DAC6_Arrangement version="2020-04-16T09:30:47Z"
                                     xmlns:dac6="urn:ukdac6:v0.1"
                                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <submission>Submitted Data</submission>
    </DAC6_Arrangement>

    val result = service.addNameSpaceDefinitions(file)

    result.toString mustBe expected.toString
  }

}