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

import java.time.LocalDateTime
import java.util.{Calendar, GregorianCalendar}
import base.SpecBase
import fixtures.XMLFixture
import models.{Dac6MetaData, SubmissionDetails, Validation}
import org.mockito.Mockito.{when, _}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import play.api.Application
import play.api.inject.bind
import repositories.SubmissionDetailsRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BusinessRuleValidationServiceSpec extends SpecBase with MockitoSugar with IntegrationPatience with BeforeAndAfterEach {

  val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]

  override def beforeEach: Unit = {
    reset(mockSubmissionDetailsRepository)
  }

  val application: Application = applicationBuilder()
    .overrides(
      bind[SubmissionDetailsRepository].toInstance(mockSubmissionDetailsRepository)
    ).build()

  val valuesForHallmarkD = List("DAC6D1Other", "DAC6D1a", "DAC6D1b", "DAC6D1c",
    "DAC6D1d", "DAC6D1e", "DAC6D1f", "DAC6D2")

  "BusinessRuleValidationService" - {
    "must be able to extract the initial disclosure when set" in {
      val xml = XMLFixture.dac6NotInitialDisclosureMA

      BusinessRuleValidationService.isInitialDisclosureMA(xml).value mustBe false
    }

    "must be able to use default initial disclosure when not set" in {
      val xml = XMLFixture.dac6InitialDisclosureMANotSet

      BusinessRuleValidationService.isInitialDisclosureMA(xml).value mustBe false
    }

    "must be able to extract relevant taxpayers" in {
      val xml = XMLFixture.dac6RelevantTaxPayers

      BusinessRuleValidationService.noOfRelevantTaxPayers(xml).value mustBe 2
    }

    "must be able to find no relevant taxpayers" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      BusinessRuleValidationService.noOfRelevantTaxPayers(xml).value mustBe 0
    }

    "must fail validation if RelevantTaxPayer date of births are before 01/01/1900" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1899-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.RelevantTaxPayersBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must fail validation if no relevant taxpayers are present for a DAC6NEW if InitialDisclosureMA is false" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <ArrangementID>GBA20200904AAAAAA</ArrangementID>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>


      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.initialDisclosure.needRelevantTaxPayer", false))
      }
    }


    "must fail validation if no relevant taxpayers are present for a DAC6ADD if InitialDisclosureMA is false" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <ArrangementID>GBA20200904AAAAAA</ArrangementID>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>


      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.initialDisclosure.needRelevantTaxPayer", false))
      }
    }

    "must pass validation if no relevant taxpayers are present for a DAC6DEL if InitialDisclosureMA is false" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <ArrangementID>GBA20200904AAAAAA</ArrangementID>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureID>AAA000000000</DisclosureID>
            <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>


      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List()
      }
    }

    "must fail validation if user is replacing a DAC6NEW where initialDisclosureMA was false and no taxpayers are provided" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <ArrangementID>GBA20200904AAAAAA</ArrangementID>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureID>AAA000000000</DisclosureID>
            <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
        "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New",
        initialDisclosureMA = false, messageRefId = "GB0000000XXX")

      when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId("GBA20200904AAAAAA"))
        .thenReturn(Future.successful(Some(firstDisclosure)))

      val service = new BusinessRuleValidationService(mockSubmissionDetailsRepository)
      val result = service.validateInitialDisclosureHasRelevantTaxPayer()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe Validation("businessrules.initialDisclosure.needRelevantTaxPayer",false,None)
      }
    }

    "must pass validation if user is replacing a DAC6NEW where initialDisclosureMA was true and no taxpayers are provided" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <ArrangementID>GBA20200904AAAAAA</ArrangementID>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureID>GBD20200904AAAAAA</DisclosureID>
            <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
        "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New",
        initialDisclosureMA = true, messageRefId = "GB0000000XXX")

      when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId("GBA20200904AAAAAA"))
        .thenReturn(Future.successful(Some(firstDisclosure)))

      val service = new BusinessRuleValidationService(mockSubmissionDetailsRepository)
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List()
      }
    }

    "must fail validation if user is replacing a DAC6ADD where initialDisclosureMA  was true in the DAC6NEW and no taxpayers are provided" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <ArrangementID>GBA20200904AAAAAA</ArrangementID>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureID>GBD20200904BBBBBB</DisclosureID>
            <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
        "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New",
        initialDisclosureMA = true, messageRefId = "GB0000000XXX")

      when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId("GBA20200904AAAAAA"))
        .thenReturn(Future.successful(Some(firstDisclosure)))

      val service = new BusinessRuleValidationService(mockSubmissionDetailsRepository)
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.initialDisclosure.needRelevantTaxPayer",false,None))
      }
    }


    "must fail validation if taxpayer implementing dates are provided for DAC6 new for non ma" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0001122345006</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing>
              <ID>
                <Organisation>
                  <OrganisationName>Tyrell Corporation</OrganisationName>
                  <TIN issuedBy="GB">AA000000D</TIN>
                  <Address>
                    <Street>Sesame</Street>
                    <BuildingIdentifier>4</BuildingIdentifier>
                    <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                    <FloorIdentifier>Second</FloorIdentifier>
                    <DistrictName>Westminster</DistrictName>
                    <POB>48</POB>
                    <PostCode>SW1A 4GG</PostCode>
                    <City>London</City>
                    <Country>GB</Country>
                  </Address>
                  <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                  <ResCountryCode>VU</ResCountryCode>
                </Organisation>
              </ID>
              <Liability>
                <IntermediaryDiscloser>
                  <IntermediaryNexus>FTNAE</IntermediaryNexus>
                  <Capacity>DAC61107</Capacity>
                  <Intermediary>
                    <ID>
                      <Individual>
                        <IndividualName>
                          <FirstName>Larry</FirstName>
                          <LastName>C</LastName>
                          <Suffix>(Cat)</Suffix>
                        </IndividualName>
                        <BirthDate>1945-01-14</BirthDate>
                        <BirthPlace>Hexham</BirthPlace>
                        <TIN issuedBy="GB">AA000000D</TIN>
                        <Address>
                          <Street>Downing Street</Street>
                          <BuildingIdentifier>No 10</BuildingIdentifier>
                          <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                          <FloorIdentifier>Second</FloorIdentifier>
                          <DistrictName>Westminster</DistrictName>
                          <POB>48</POB>
                          <PostCode>SW1A 4GG</PostCode>
                          <City>London</City>
                          <Country>GB</Country>
                        </Address>
                        <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                        <ResCountryCode>VU</ResCountryCode>
                      </Individual>
                    </ID>
                    <Capacity>DAC61102</Capacity>
                    <NationalExemption>
                      <Exemption></Exemption>
                      <CountryExemptions>
                        <CountryExemption>VU</CountryExemption>
                      </CountryExemptions>
                    </NationalExemption>
                  </Intermediary>
                </IntermediaryDiscloser>
              </Liability>
            </Disclosing>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <ID>
                  <Individual>
                    <IndividualName>
                      <FirstName>Larry</FirstName>
                      <LastName>C</LastName>
                      <Suffix>(Cat)</Suffix>
                    </IndividualName>
                    <BirthDate>1994-04-25</BirthDate>
                    <BirthPlace>Petrol Station</BirthPlace>
                    <TIN issuedBy="GB">AA000000A</TIN>
                    <Address>
                      <Street>Downing Street</Street>
                      <BuildingIdentifier>No 10</BuildingIdentifier>
                      <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                      <FloorIdentifier>Second</FloorIdentifier>
                      <DistrictName>Westminster</DistrictName>
                      <POB>48</POB>
                      <PostCode>SW1A 4GG</PostCode>
                      <City>London</City>
                      <Country>GB</Country>
                    </Address>
                    <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                    <ResCountryCode>VU</ResCountryCode>
                  </Individual>
                </ID>
                <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
                <AssociatedEnterprises>
                  <AssociatedEnterprise>
                    <AssociatedEnterpriseID>
                      <Individual>
                        <IndividualName>
                          <FirstName>Larry</FirstName>
                          <LastName>C</LastName>
                          <Suffix>(Cat)</Suffix>
                        </IndividualName>
                        <BirthDate>2007-01-14</BirthDate>
                        <BirthPlace>Hexham</BirthPlace>
                        <TIN issuedBy="GB">AA000000D</TIN>
                        <Address>
                          <Street>Downing Street</Street>
                          <BuildingIdentifier>No 10</BuildingIdentifier>
                          <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                          <FloorIdentifier>Second</FloorIdentifier>
                          <DistrictName>Westminster</DistrictName>
                          <POB>48</POB>
                          <PostCode>SW1A 4GG</PostCode>
                          <City>London</City>
                          <Country>GB</Country>
                        </Address>
                        <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                        <ResCountryCode>VU</ResCountryCode>
                      </Individual>
                    </AssociatedEnterpriseID>
                    <AffectedPerson>true</AffectedPerson>
                  </AssociatedEnterprise>
                </AssociatedEnterprises>
              </RelevantTaxpayer>
            </RelevantTaxPayers>
            <Intermediaries>
              <Intermediary>
                <ID>
                  <Individual>
                    <IndividualName>
                      <FirstName>Larry</FirstName>
                      <LastName>C</LastName>
                      <Suffix>(Cat)</Suffix>
                    </IndividualName>
                    <BirthDate>2007-01-14</BirthDate>
                    <BirthPlace>Hexham</BirthPlace>
                    <TIN issuedBy="GB">AA000000D</TIN>
                    <Address>
                      <Street>Downing Street</Street>
                      <BuildingIdentifier>No 10</BuildingIdentifier>
                      <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                      <FloorIdentifier>Second</FloorIdentifier>
                      <DistrictName>Westminster</DistrictName>
                      <POB>48</POB>
                      <PostCode>SW1A 4GG</PostCode>
                      <City>London</City>
                      <Country>GB</Country>
                    </Address>
                    <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                    <ResCountryCode>VU</ResCountryCode>
                  </Individual>
                </ID>
                <Capacity>DAC61102</Capacity>
                <NationalExemption>
                  <Exemption>true</Exemption>
                  <CountryExemptions>
                    <CountryExemption>VU</CountryExemption>
                  </CountryExemptions>
                </NationalExemption>
              </Intermediary>
            </Intermediaries>
            <AffectedPersons>
              <AffectedPerson>
                <AffectedPersonID>
                  <Individual>
                    <IndividualName>
                      <FirstName>Palmerston</FirstName>
                      <LastName>C</LastName>
                      <Suffix>(Cat)</Suffix>
                    </IndividualName>
                    <BirthDate>2012-01-14</BirthDate>
                    <BirthPlace>Hexham</BirthPlace>
                    <TIN issuedBy="GB">AB000000D</TIN>
                    <Address>
                      <Street>King Charles Street</Street>
                      <BuildingIdentifier>No 10</BuildingIdentifier>
                      <SuiteIdentifier>Lord Palmerston Suite</SuiteIdentifier>
                      <FloorIdentifier>Second</FloorIdentifier>
                      <DistrictName>Westminster</DistrictName>
                      <POB>48</POB>
                      <PostCode>SW1A 4GG</PostCode>
                      <City>London</City>
                      <Country>GB</Country>
                    </Address>
                    <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                    <ResCountryCode>VU</ResCountryCode>
                  </Individual>
                </AffectedPersonID>
              </AffectedPerson>
            </AffectedPersons>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", false))
      }
    }

    "must fail validation if disclosing date of births are before 01/01/1900" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <Disclosing>
            <ID>
              <Individual>
                <IndividualName>
                  <FirstName>John</FirstName>
                  <LastName>Charles</LastName>
                  <Suffix>Mr</Suffix>
                </IndividualName>
                <BirthDate>1899-12-31</BirthDate>
                <BirthPlace>Random Town</BirthPlace>
                <TIN issuedBy="GB">AA000000D</TIN>
                <Address>
                  <Street>Random Street</Street>
                  <BuildingIdentifier>No 10</BuildingIdentifier>
                  <SuiteIdentifier>Random Suite</SuiteIdentifier>
                  <FloorIdentifier>Second</FloorIdentifier>
                  <DistrictName>Random District</DistrictName>
                  <POB>48</POB>
                  <PostCode>SW1A 4GG</PostCode>
                  <City>Random City</City>
                  <Country>GB</Country>
                </Address>
                <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                <ResCountryCode>VU</ResCountryCode>
              </Individual>
            </ID>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
          </Disclosing>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1988-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.DisclosingBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must fail validation if AssociatedEnterprise date of births are on or after 01/01/1900" in {

      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1988-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
          <AssociatedEnterprises>
            <AssociatedEnterprise>
              <AssociatedEnterpriseID>
                <Individual>
                  <IndividualName>
                    <FirstName>Name</FirstName>
                    <LastName>C</LastName>
                    <Suffix>(Cat)</Suffix>
                  </IndividualName>
                  <BirthDate>1899-12-31</BirthDate>
                  <BirthPlace>BirthPlace</BirthPlace>
                  <TIN issuedBy="GB">AA000000D</TIN>
                  <Address>
                    <Street>Street</Street>
                    <BuildingIdentifier>No 10</BuildingIdentifier>
                    <SuiteIdentifier>Suite</SuiteIdentifier>
                    <FloorIdentifier>Second</FloorIdentifier>
                    <DistrictName>DistrictName</DistrictName>
                    <POB>48</POB>
                    <PostCode>SW1A 4GG</PostCode>
                    <City>London</City>
                    <Country>GB</Country>
                  </Address>
                  <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                  <ResCountryCode>VU</ResCountryCode>
                </Individual>
              </AssociatedEnterpriseID>
              <AffectedPerson>true</AffectedPerson>
            </AssociatedEnterprise>
          </AssociatedEnterprises>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.AssociatedEnterprisesBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must fail validation if intermediary date of births are before 01/01/1900" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1988-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
            <Intermediaries>
              <Intermediary>
                <ID>
                  <Individual>
                    <IndividualName>
                      <FirstName>Larry</FirstName>
                      <LastName>C</LastName>
                      <Suffix>DDD</Suffix>
                    </IndividualName>
                    <BirthDate>1899-12-31</BirthDate>
                    <BirthPlace>BirthPlace</BirthPlace>
                    <TIN issuedBy="GB">AA000000D</TIN>
                    <Address>
                      <Street>Downing Street</Street>
                      <BuildingIdentifier>No 10</BuildingIdentifier>
                      <SuiteIdentifier>Suite</SuiteIdentifier>
                      <FloorIdentifier>Second</FloorIdentifier>
                      <DistrictName>DistrictName</DistrictName>
                      <POB>48</POB>
                      <PostCode>SW1A 4GG</PostCode>
                      <City>London</City>
                      <Country>GB</Country>
                    </Address>
                    <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                    <ResCountryCode>VU</ResCountryCode>
                  </Individual>
                </ID>
                <Capacity>DAC61102</Capacity>
                <NationalExemption>
                  <Exemption>true</Exemption>
                  <CountryExemptions>
                    <CountryExemption>VU</CountryExemption>
                  </CountryExemptions>
                </NationalExemption>
              </Intermediary>
            </Intermediaries>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.IntermediaryBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must fail validation if affected persons date of births are before 01/01/1900" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1988-12-31</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
            <AffectedPersons>
              <AffectedPerson>
                <AffectedPersonID>
                  <Individual>
                    <IndividualName>
                      <FirstName>FirstName</FirstName>
                      <LastName>LastName</LastName>
                      <Suffix>Suffix</Suffix>
                    </IndividualName>
                    <BirthDate>1899-12-31</BirthDate>
                    <BirthPlace>BirthPlace</BirthPlace>
                    <TIN issuedBy="GB">AB000000D</TIN>
                    <Address>
                      <Street>Street</Street>
                      <BuildingIdentifier>No 10</BuildingIdentifier>
                      <SuiteIdentifier>BuildingIdentifier</SuiteIdentifier>
                      <FloorIdentifier>Second</FloorIdentifier>
                      <DistrictName>DistrictName</DistrictName>
                      <POB>48</POB>
                      <PostCode>SW1A 4GG</PostCode>
                      <City>City</City>
                      <Country>GB</Country>
                    </Address>
                    <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                    <ResCountryCode>VU</ResCountryCode>
                  </Individual>
                </AffectedPersonID>
              </AffectedPerson>
            </AffectedPersons>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.AffectedPersonsBirthDates.maxDateOfBirthExceeded", false))
      }
    }

    "must pass validation if date of births are on a after 01/01/1900" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <BirthDate>1900-01-01</BirthDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List()
      }
    }

    "must pass validation if an initial disclosure marketable arrangement has one or more relevant taxpayers" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>true</InitialDisclosureMA>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      val service = app.injector.instanceOf[BusinessRuleValidationService]

      val result = service.validateInitialDisclosureHasRelevantTaxPayer()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _.value mustBe true
      }
    }

    "must not pass validation if an initial disclosure marketable arrangement does not have one or more relevant taxpayers" in {
      val xml =
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

      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateInitialDisclosureHasRelevantTaxPayer()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _.value mustBe false
      }

    }

    "must correctly report presence of RelevantTaxpayerDiscloser" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing>
              <Liability>
                <RelevantTaxpayerDiscloser>
                  <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                  <Capacity>DAC61105</Capacity>
                </RelevantTaxpayerDiscloser>
              </Liability>
            </Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      BusinessRuleValidationService.hasRelevantTaxpayerDiscloser(xml).value mustBe true
    }

    "must correctly report absence of RelevantTaxpayerDiscloser" in {
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Disclosing>
              <Liability>
                <IntermediaryDiscloser>
                  <IntermediaryNexus>INEXb</IntermediaryNexus>
                  <Capacity>DAC61101</Capacity>
                </IntermediaryDiscloser>
              </Liability>
            </Disclosing>
          </DAC6Disclosures>
        </DAC6_Arrangement>

      BusinessRuleValidationService.hasRelevantTaxpayerDiscloser(xml).value mustBe false
    }
  }

  "must correctly validate a RelevantTaxPayer with a RelevantTaxpayerDiscloser" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateRelevantTaxpayerDiscloserHasRelevantTaxPayer()(xml).get.value mustBe true
  }

  "must correctly fail validation for a RelevantTaxpayerDiscloser without a RelevantTaxPayer" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateRelevantTaxpayerDiscloserHasRelevantTaxPayer()(xml).get.value mustBe false
  }

  "must correctly report presence of IntermediaryDiscloser" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasIntermediaryDiscloser(xml).value mustBe true
  }

  "must correctly report absence of IntermediaryDiscloser" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasIntermediaryDiscloser(xml).value mustBe false
  }

  "must correctly count the number of intermediaries" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
            <Intermediaries>
              <Intermediary></Intermediary>
            </Intermediaries>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.noOfIntermediaries(xml).value mustBe 1
  }

  "must correctly report absence of Intermediary" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.noOfIntermediaries(xml).value mustBe 0
  }

  "must correctly validate intermediaries when intermediary discloser is set" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
            <Intermediaries>
              <Intermediary></Intermediary>
            </Intermediaries>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateIntermediaryDiscloserHasIntermediary()(xml).get.value mustBe true
  }

  "must correctly invalidate intermediaries when intermediary discloser is set" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>INEXb</IntermediaryNexus>
                <Capacity>DAC61101</Capacity>
              </IntermediaryDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateIntermediaryDiscloserHasIntermediary()(xml).get.value mustBe false
  }

  "must correctly validate intermediaries when intermediary discloser is not set" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer></RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateIntermediaryDiscloserHasIntermediary()(xml).get.value mustBe true
  }

  "must correctly extract TaxpayerImplementingDates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2020-06-21</TaxpayerImplementingDate>
              </RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.taxPayerImplementingDates(xml).value mustBe Seq(
      new GregorianCalendar(2020, Calendar.MAY, 14).getTime,
      new GregorianCalendar(2020, Calendar.JUNE, 21).getTime,
    )
  }

  "must correctly invalidate mixed TaxpayerImplementingDates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2018-05-14</TaxpayerImplementingDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2018-06-26</TaxpayerImplementingDate>
              </RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateAllTaxpayerImplementingDatesAreAfterStart()(xml).get.value mustBe false
  }

  "must correctly validate with TaxpayerImplementingDate equal to start" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
              </RelevantTaxpayer>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>2018-06-25</TaxpayerImplementingDate>
              </RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateAllTaxpayerImplementingDatesAreAfterStart()(xml).get.value mustBe true
  }

  "must correctly extract ImplementingDates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-01-21</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureInformationImplementingDates(xml).value mustBe Seq(
      new GregorianCalendar(2020, Calendar.JANUARY, 14).getTime,
      new GregorianCalendar(2018, Calendar.JANUARY, 21).getTime,
    )
  }

  "must correctly extract ImplementingDates when absent" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
          </DisclosureInformation>
          <DisclosureInformation>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureInformationImplementingDates(xml).value mustBe Seq.empty
  }

  "must correctly invalidate mixed ImplementingDates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-01-21</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateAllImplementingDatesAreAfterStart()(xml).get.value mustBe false
  }

  "must correctly validate with ImplementingDate equal to start" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateAllImplementingDatesAreAfterStart()(xml).get.value mustBe true
  }

  "must correctly extract DisclosureImportInstruction" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureImportInstruction(xml).value mustBe "DAC6NEW"
  }

  "must correctly extract DisclosureID when present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureID(xml).value mustBe "AAA000000000"
  }

  "must correctly extract DisclosureID when not present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.disclosureID(xml).value mustBe ""
  }

  "must correctly extract ArrangementID when present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.arrangementID(xml).value mustBe "AAA000000000"
  }

  "must correctly extract ArrangementID when not present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.arrangementID(xml).value mustBe ""
  }

  "must correctly validate with New Disclosure without ArrangementID or DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe true
  }

  "must correctly invalidate with New Disclosure with an ArrangementID but no DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly invalidate with New Disclosure with no ArrangementID but a DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly invalidate with New Disclosure with ArrangementID and a DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Add Disclosure with ArrangementID and no DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe true
  }

  "must correctly invalidate with Add Disclosure with ArrangementID and DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly invalidate with Add Disclosure with InitialDisclosureMA set to true" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstructionAndInitialDisclosureFlag()(xml).get.value mustBe false

  }

  "must correctly invalidate with Add Disclosure no ArrangementID but a DisclosureID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly extract MessageRefID when present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.messageRefID(xml).value mustBe "GB0000000XXX"
  }

  "must correctly validate with Rep Disclosure with ArrangementID and DisclosureID and MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe true
  }

  "must correctly validate with Rep Disclosure with ArrangementID no DisclosureID and with MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Rep Disclosure with ArrangementID and DisclosureID and no MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Rep Disclosure with no ArrangementID and with DisclosureID and MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Del Disclosure with ArrangementID and DisclosureID and MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe true
  }

  "must correctly validate with Del Disclosure with ArrangementID no DisclosureID and with MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Del Disclosure with ArrangementID and DisclosureID and no MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate with Del Disclosure with no ArrangementID and with DisclosureID and MessageRefID" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDisclosureImportInstruction()(xml).get.value mustBe false
  }

  "must correctly validate an initial disclosure MA with Relevant Tax Payers has a TaxPayer Implementation Dates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe true
    }  }

  "must correctly invalidate an initial disclosure MA with Relevant Tax Payers has a missing TaxPayer Implementation Date" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe false
    }  }

  "must correctly validate an non initial disclosure MA with Relevant Tax Payers has a missing TaxPayer Implementation Date" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe true
    }  }

  "must correctly invalidate an initial disclosure MA with Relevant Tax Payers that has a missing TaxPayer Implementation Date" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List(Validation("businessrules.initialDisclosureMA.missingRelevantTaxPayerDates", false))
    }
  }

  "must correctly validate a DAC6DEL MA with Relevant Tax Payers that has a missing TaxPayer Implementation Date" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <ArrangementID>AAA000000000</ArrangementID>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List()
    }
  }

  "must correctly extract the hallmarks" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6A1</Hallmark>
                <Hallmark>DAC6A3</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hallmarks(xml).value mustBe Seq("DAC6A1", "DAC6A3")
  }

  "must correctly validate the hallmarks when MainBenefitTest1 is set" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <MainBenefitTest1>true</MainBenefitTest1>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6A1</Hallmark>
                <Hallmark>DAC6A3</Hallmark>
                <Hallmark>DAC6C1c</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe true
    }  }

  "must correctly validate a file has TaxPayer Implementation Dates if initial disclosure MA is false, " +
    "first disclosure for arrangement ID is not found and Relevant Tax Payers exist" in {

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe true
    }
  }

  "must correctly validate a file has TaxPayer Implementation Dates if initial disclosure MA is false, " +
    "first disclosure's InitialDisclosureMA is true and Relevant Tax Payers exist" in {

    val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New",
      initialDisclosureMA = true, messageRefId = "GB0000000XXX")

    when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(Some(firstDisclosure)))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe true
    }
  }

  "must correctly validate a file that has TaxPayer Implementation Dates if initial disclosure MA is false for DAC6NEW" in {

    val replacedFirstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "Replace",
      initialDisclosureMA = false, messageRefId = "GB0000000XXX")

    when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(Some(replacedFirstDisclosure)))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", true)
    }
  }

  "must correctly invalidate a DAC6ADD for a non-marketable arrangment where user has putTaxPayer Implementation Dates" in {

    val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New",
      initialDisclosureMA = false, messageRefId = "GB0000000XXX")

    when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(Some(firstDisclosure)))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", false)
    }
  }

  "must correctly invalidate a DAC6REP for a non-marketable arrangement where user has putTaxPayer Implementation Dates" in {

    val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New",
      initialDisclosureMA = false, messageRefId = "GB0000000XXX")

    when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(Some(firstDisclosure)))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", false)
    }
  }


  "must correctly invalidate a file with missing TaxPayer Implementation Dates if initial disclosure MA is false, " +
    "first disclosure's InitialDisclosureMA is true and Relevant Tax Payers exist" in {

    val firstDisclosure: SubmissionDetails = SubmissionDetails("enrolmentID", LocalDateTime.parse("2020-05-14T17:10:00"),
      "fileName", Some("GBA20200904AAAAAA"), Some("GBD20200904AAAAAA"), "New",
      initialDisclosureMA = true, messageRefId = "GB0000000XXX")

    when(mockSubmissionDetailsRepository.retrieveFirstDisclosureForArrangementId("GBA20200904AAAAAA"))
      .thenReturn(Future.successful(Some(firstDisclosure)))

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>GBA20200904AAAAAA</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2019-05-15</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = application.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateTaxPayerImplementingDateAgainstMarketableArrangementStatus()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _.value mustBe false
    }
  }

  "must extract presence of DAC6D1OtherInfo" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6C1bii</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasDAC6D1OtherInfo(xml).value mustBe true
  }

  "must extract absence of DAC6D1OtherInfo" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6C1bii</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    BusinessRuleValidationService.hasDAC6D1OtherInfo(xml).value mustBe false
  }

  "must correctly validate that other info is provided when hallmark present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6D1Other</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDAC6D1OtherInfoHasNecessaryHallmark()(xml).get.value mustBe true
  }

  "must recover from exception if implementing date is not in parseable format" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>true</InitialDisclosureMA>
            <ImplementingDate>wrong format</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6D1Other</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List()
    }
  }

  "must recover from exception if taxpayerImplementing date is not in parseable format" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <InitialDisclosureMA>true</InitialDisclosureMA>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <Liability>
              <RelevantTaxpayerDiscloser>
                <RelevantTaxpayerNexus>RTNEXb</RelevantTaxpayerNexus>
                <Capacity>DAC61105</Capacity>
              </RelevantTaxpayerDiscloser>
            </Liability>
            <RelevantTaxPayers>
              <RelevantTaxpayer>
                <TaxpayerImplementingDate>wrong format</TaxpayerImplementingDate>
              </RelevantTaxpayer>
            </RelevantTaxPayers>
          </Disclosing>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List()
    }
  }

  "must report correct dob of birth errors" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <Disclosing>
            <ID>
              <Organisation>
                <OrganisationName>Tyrell Corporation</OrganisationName>
                <TIN issuedBy="GB">AA000000D</TIN>
                <Address>
                  <Street>Sesame</Street>
                  <BuildingIdentifier>4</BuildingIdentifier>
                  <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                  <FloorIdentifier>Second</FloorIdentifier>
                  <DistrictName>Westminster</DistrictName>
                  <POB>48</POB>
                  <PostCode>SW1A 4GG</PostCode>
                  <City>London</City>
                  <Country>GB</Country>
                </Address>
                <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                <ResCountryCode>VU</ResCountryCode>
              </Organisation>
            </ID>
            <Liability>
              <IntermediaryDiscloser>
                <IntermediaryNexus>FTNAE</IntermediaryNexus>
                <Capacity>DAC61107</Capacity>
                <Intermediary>
                  <ID>
                    <Individual>
                      <IndividualName>
                        <FirstName>Larry</FirstName>
                        <LastName>C</LastName>
                        <Suffix>(Cat)</Suffix>
                      </IndividualName>
                      <BirthDate>1899-01-14</BirthDate>
                      <BirthPlace>Hexham</BirthPlace>
                      <TIN issuedBy="GB">AA000000D</TIN>
                      <Address>
                        <Street>Downing Street</Street>
                        <BuildingIdentifier>No 10</BuildingIdentifier>
                        <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                        <FloorIdentifier>Second</FloorIdentifier>
                        <DistrictName>Westminster</DistrictName>
                        <POB>48</POB>
                        <PostCode>SW1A 4GG</PostCode>
                        <City>London</City>
                        <Country>GB</Country>
                      </Address>
                      <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                      <ResCountryCode>VU</ResCountryCode>
                    </Individual>
                  </ID>
                  <Capacity>DAC61102</Capacity>
                  <NationalExemption>
                    <Exemption></Exemption>
                    <CountryExemptions>
                      <CountryExemption>VU</CountryExemption>
                    </CountryExemptions>
                  </NationalExemption>
                </Intermediary>
              </IntermediaryDiscloser>
            </Liability>
          </Disclosing>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <ID>
                <Individual>
                  <IndividualName>
                    <FirstName>Larry</FirstName>
                    <LastName>C</LastName>
                    <Suffix>(Cat)</Suffix>
                  </IndividualName>
                  <BirthDate>1994-04-25</BirthDate>
                  <BirthPlace>Petrol Station</BirthPlace>
                  <TIN issuedBy="GB">AA000000A</TIN>
                  <Address>
                    <Street>Downing Street</Street>
                    <BuildingIdentifier>No 10</BuildingIdentifier>
                    <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                    <FloorIdentifier>Second</FloorIdentifier>
                    <DistrictName>Westminster</DistrictName>
                    <POB>48</POB>
                    <PostCode>SW1A 4GG</PostCode>
                    <City>London</City>
                    <Country>GB</Country>
                  </Address>
                  <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                  <ResCountryCode>VU</ResCountryCode>
                </Individual>
              </ID>
              <AssociatedEnterprises>
                <AssociatedEnterprise>
                  <AssociatedEnterpriseID>
                    <Individual>
                      <IndividualName>
                        <FirstName>Larry</FirstName>
                        <LastName>C</LastName>
                        <Suffix>(Cat)</Suffix>
                      </IndividualName>
                      <BirthDate>2007-01-14</BirthDate>
                      <BirthPlace>Hexham</BirthPlace>
                      <TIN issuedBy="GB">AA000000D</TIN>
                      <Address>
                        <Street>Downing Street</Street>
                        <BuildingIdentifier>No 10</BuildingIdentifier>
                        <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                        <FloorIdentifier>Second</FloorIdentifier>
                        <DistrictName>Westminster</DistrictName>
                        <POB>48</POB>
                        <PostCode>SW1A 4GG</PostCode>
                        <City>London</City>
                        <Country>GB</Country>
                      </Address>
                      <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                      <ResCountryCode>VU</ResCountryCode>
                    </Individual>
                  </AssociatedEnterpriseID>
                  <AffectedPerson>true</AffectedPerson>
                </AssociatedEnterprise>
              </AssociatedEnterprises>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
          <Intermediaries>
            <Intermediary>
              <ID>
                <Individual>
                  <IndividualName>
                    <FirstName>Larry</FirstName>
                    <LastName>C</LastName>
                    <Suffix>(Cat)</Suffix>
                  </IndividualName>
                  <BirthDate>2007-01-14</BirthDate>
                  <BirthPlace>Hexham</BirthPlace>
                  <TIN issuedBy="GB">AA000000D</TIN>
                  <Address>
                    <Street>Downing Street</Street>
                    <BuildingIdentifier>No 10</BuildingIdentifier>
                    <SuiteIdentifier>Sir Humphrey Suite</SuiteIdentifier>
                    <FloorIdentifier>Second</FloorIdentifier>
                    <DistrictName>Westminster</DistrictName>
                    <POB>48</POB>
                    <PostCode>SW1A 4GG</PostCode>
                    <City>London</City>
                    <Country>GB</Country>
                  </Address>
                  <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                  <ResCountryCode>VU</ResCountryCode>
                </Individual>
              </ID>
              <Capacity>DAC61102</Capacity>
              <NationalExemption>
                <Exemption>true</Exemption>
                <CountryExemptions>
                  <CountryExemption>VU</CountryExemption>
                </CountryExemptions>
              </NationalExemption>
            </Intermediary>
          </Intermediaries>
          <AffectedPersons>
            <AffectedPerson>
              <AffectedPersonID>
                <Individual>
                  <IndividualName>
                    <FirstName>Palmerston</FirstName>
                    <LastName>C</LastName>
                    <Suffix>(Cat)</Suffix>
                  </IndividualName>
                  <BirthDate>2012-01-14</BirthDate>
                  <BirthPlace>Hexham</BirthPlace>
                  <TIN issuedBy="GB">AB000000D</TIN>
                  <Address>
                    <Street>King Charles Street</Street>
                    <BuildingIdentifier>No 10</BuildingIdentifier>
                    <SuiteIdentifier>Lord Palmerston Suite</SuiteIdentifier>
                    <FloorIdentifier>Second</FloorIdentifier>
                    <DistrictName>Westminster</DistrictName>
                    <POB>48</POB>
                    <PostCode>SW1A 4GG</PostCode>
                    <City>London</City>
                    <Country>GB</Country>
                  </Address>
                  <EmailAddress>test@digital.hmrc.gov.uk</EmailAddress>
                  <ResCountryCode>VU</ResCountryCode>
                </Individual>
              </AffectedPersonID>
            </AffectedPerson>
          </AffectedPersons>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List(Validation("businessrules.DisclosingBirthDates.maxDateOfBirthExceeded",false,None))

    }
  }

  "must correctly invalidate that other info is provided when hallmark absent" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6C4</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.validateDAC6D1OtherInfoHasNecessaryHallmark()(xml).get.value mustBe false
  }
  "must return correct errors for xml with mutiple errors: " +
    " error 1 = initial disclosure marketable arrangement does not have one or more relevant taxpayers " +
    " error 2 = other info is provided when hallmark absent" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <Hallmarks>
            <ListHallmarks>
              <Hallmark>DAC6D1a</Hallmark>
            </ListHallmarks>
            <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
          </Hallmarks>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List(Validation("businessrules.initialDisclosure.needRelevantTaxPayer", false),
        Validation("businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo", false))
    }
  }

  "must return no errors for valid xml" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <InitialDisclosureMA>true</InitialDisclosureMA>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6D1Other</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-06-21</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List()
    }
  }

  "must return correct metadata for import instruction DAC6NEW when disclosure info is present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
            <Reason>DAC6704</Reason>
            <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6D1Other</Hallmark>
              </ListHallmarks>
              <DAC6D1OtherInfo>Some Text</DAC6D1OtherInfo>
            </Hallmarks>
          </DisclosureInformation>
        </DAC6Disclosures>
        <InitialDisclosureMA>true</InitialDisclosureMA>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6NEW", None, None,
      disclosureInformationPresent = true, initialDisclosureMA = true,
      messageRefId = "GB0000000XXX"))
  }

  "must pass validation for all allowed hallmark D values" in {

    valuesForHallmarkD.foreach { value =>
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <ArrangementID>GBA20200904AAAAAA</ArrangementID>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureID>AAA000000000</DisclosureID>
            <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
            </RelevantTaxPayers>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>{value}</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DAC6Disclosures>
        </DAC6_Arrangement>


      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List()
      }
    }
  }

  "must fail validation for non hallmark D value" in {

    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <ArrangementID>GBA20200904AAAAAA</ArrangementID>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DAC6Disclosures>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <InitialDisclosureMA>false</InitialDisclosureMA>
          <RelevantTaxPayers>
          </RelevantTaxPayers>
          <MainBenefitTest1>true</MainBenefitTest1>
          <Hallmarks>
            <ListHallmarks>
              <Hallmark>DAC6A1</Hallmark>
            </ListHallmarks>
          </Hallmarks>
        </DAC6Disclosures>
      </DAC6_Arrangement>


    val service = app.injector.instanceOf[BusinessRuleValidationService]
    val result = service.validateFile()(implicitly, implicitly)(xml)

    whenReady(result.get) {
      _ mustBe List(Validation("businessrules.hallmarks.dHallmarkNotProvided", false))
    }
  }

  "must fail validation when non hallmark D value supplied along with hallmarkD value" in {

    valuesForHallmarkD.foreach { value =>
      val xml =
        <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
          <Header>
            <ArrangementID>GBA20200904AAAAAA</ArrangementID>
            <MessageRefId>GB0000000XXX</MessageRefId>
            <Timestamp>2020-05-14T17:10:00</Timestamp>
          </Header>
          <DAC6Disclosures>
            <DisclosureID>AAA000000000</DisclosureID>
            <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
            <InitialDisclosureMA>false</InitialDisclosureMA>
            <RelevantTaxPayers>
            </RelevantTaxPayers>
            <MainBenefitTest1>true</MainBenefitTest1>
            <Hallmarks>
              <ListHallmarks>
                <Hallmark>DAC6A1</Hallmark>
                <Hallmark>{value}</Hallmark>
              </ListHallmarks>
            </Hallmarks>
          </DAC6Disclosures>
        </DAC6_Arrangement>


      val service = app.injector.instanceOf[BusinessRuleValidationService]
      val result = service.validateFile()(implicitly, implicitly)(xml)

      whenReady(result.get) {
        _ mustBe List(Validation("businessrules.hallmarks.dHallmarkWithOtherHallmarks", false))
      }
    }
  }

  "must return correct metadata for import instruction DAC6NEW  when disclosure info is not present" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        <DAC6Disclosures>
        </DAC6Disclosures>
      </DAC6_Arrangement>
    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6NEW", None, None,
      disclosureInformationPresent = false, initialDisclosureMA = false,
      messageRefId = "GB0000000XXX"))
  }

  "must return correct metadata for import instruction DAC6ADD" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6ADD", Some("AAA000000000"), None,
      disclosureInformationPresent = true, initialDisclosureMA = false,
      messageRefId = "GB0000000XXX"))
  }

  "must return correct metadata for import instruction DAC6ADD with RelevantTaxpayers who all have implementing dates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-06-21</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6ADD", Some("AAA000000000"), None,
      disclosureInformationPresent = true, initialDisclosureMA = false,
      messageRefId = "GB0000000XXX"))
  }


  "must return correct metadata for import instruction DAC6ADD with RelevantTaxpayers who do not all have implementing dates" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6ADD</DisclosureImportInstruction>
          <RelevantTaxPayers>
            <RelevantTaxpayer>
            </RelevantTaxpayer>
            <RelevantTaxpayer>
              <TaxpayerImplementingDate>2020-06-21</TaxpayerImplementingDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6ADD", Some("AAA000000000"), None,
      disclosureInformationPresent = true, initialDisclosureMA = false,
      messageRefId = "GB0000000XXX"))
  }

  "must return correct metadata for import instruction DAC6REP" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6REP</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6REP", Some("AAA000000000"), Some("AAA000000000"),
      disclosureInformationPresent = true,
      initialDisclosureMA = false, messageRefId = "GB0000000XXX"))
  }

  "must return correct metadata for import instruction DAC6DEL" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureImportInstruction>DAC6DEL</DisclosureImportInstruction>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    service.extractDac6MetaData()(xml) mustBe Some(Dac6MetaData("DAC6DEL", Some("AAA000000000"), Some("AAA000000000"),
      disclosureInformationPresent = true, initialDisclosureMA = false,
      messageRefId = "GB0000000XXX"))
  }
  "must throw exception if disclosureImportInstruction is invalid or missing" in {
    val xml =
      <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
        <Header>
          <MessageRefId>GB0000000XXX</MessageRefId>
          <Timestamp>2020-05-14T17:10:00</Timestamp>
        </Header>
        <ArrangementID>AAA000000000</ArrangementID>
        <DAC6Disclosures>
          <DisclosureID>AAA000000000</DisclosureID>
          <DisclosureInformation>
            <ImplementingDate>2020-01-14</ImplementingDate>
          </DisclosureInformation>
          <DisclosureInformation>
            <ImplementingDate>2018-06-25</ImplementingDate>
          </DisclosureInformation>
        </DAC6Disclosures>
      </DAC6_Arrangement>

    val service = app.injector.instanceOf[BusinessRuleValidationService]
    a[RuntimeException] mustBe thrownBy {
      service.extractDac6MetaData()(xml)
    }
  }
}
