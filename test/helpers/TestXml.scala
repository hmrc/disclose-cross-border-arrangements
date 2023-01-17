/*
 * Copyright 2023 HM Revenue & Customs
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

package helpers

import scala.xml.Elem

trait TestXml {

  val otherInfoPopulatedXml =
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

  val initialDisclosureNoRelevantTaxpyersXml =
    <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
      <Header>
        <MessageRefId>GB0000000XXX</MessageRefId>
        <Timestamp>2020-05-14T17:10:00</Timestamp>
      </Header>
      <DAC6Disclosures>
        <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        <InitialDisclosureMA>true</InitialDisclosureMA>
      </DAC6Disclosures>
    </DAC6_Arrangement>

  val relevantTaxPayerDiscloserXml =
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

  val invalidDatesOfBirthXml =
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
              <BirthDate>1902-12-31</BirthDate>
            </RelevantTaxpayer>
          </RelevantTaxPayers>
          <Intermediaries>
            <Intermediary></Intermediary>
          </Intermediaries>
        </Disclosing>
        <AffectedPersons>
          <AffectedPerson>
          </AffectedPerson>
        </AffectedPersons>
        <AssociatedEnterprises>
          <AssociatedEnterprise>
            <AssociatedEnterpriseID>
              <Individual>
                <IndividualName>
                  <FirstName>Name</FirstName>
                  <LastName>C</LastName>
                  <Suffix>(Cat)</Suffix>
                </IndividualName>
                <BirthDate>1902-12-31</BirthDate>
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
      </DAC6Disclosures>
    </DAC6_Arrangement>

  val intermediaryDiscloserXml =
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

  val implementingDateAfterStartDateXml =
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

  val importInstructionErrorXml =
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
      <DisclosureID>AAA000000000</DisclosureID>
    </DAC6_Arrangement>

  val importInstructionDAC6DELErrorXml =
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
      <DisclosureID>AAA000000000</DisclosureID>
    </DAC6_Arrangement>

  val missingTaxPayerImplementingDateXml =
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
          </RelevantTaxpayer>
        </RelevantTaxPayers>
      </DAC6Disclosures>
    </DAC6_Arrangement>

  val mainBenefitTestErrorXml =
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
            </ListHallmarks>
          </Hallmarks>
        </DisclosureInformation>
      </DAC6Disclosures>
    </DAC6_Arrangement>

  val validXml: Elem =
    <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
      <Header>
        <MessageRefId>GB0000000XXX</MessageRefId>
        <Timestamp>2020-05-14T17:10:00</Timestamp>
      </Header>
      <DAC6Disclosures>
        <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        <Disclosing>
          <ID>
            <Individual>
              <IndividualName>
                <FirstName>John</FirstName>
                <LastName>Charles</LastName>
                <Suffix>Mr</Suffix>
              </IndividualName>
              <BirthDate>2007-01-14</BirthDate>
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
        <InitialDisclosureMA></InitialDisclosureMA>
        <RelevantTaxPayers>
          <RelevantTaxpayer>
            <ID>
              <Individual>
                <IndividualName>
                  <FirstName>Larry</FirstName>
                  <LastName>C</LastName>
                  <Suffix>Mr</Suffix>
                </IndividualName>
                <BirthDate>2007-01-14</BirthDate>
                <BirthPlace>Birthplace</BirthPlace>
                <TIN issuedBy="GB">AA000000D</TIN>
                <Address>
                  <Street>Street</Street>
                  <BuildingIdentifier>1</BuildingIdentifier>
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
            <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
            <AssociatedEnterprises>
              <AssociatedEnterprise>
                <AssociatedEnterpriseID>
                  <Individual>
                    <IndividualName>
                      <FirstName>Name</FirstName>
                      <LastName>C</LastName>
                      <Suffix>(Cat)</Suffix>
                    </IndividualName>
                    <BirthDate>2007-01-14</BirthDate>
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
          </RelevantTaxpayer>
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
                <BirthDate>2007-01-14</BirthDate>
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
        <AffectedPersons>
          <AffectedPerson>
            <AffectedPersonID>
              <Individual>
                <IndividualName>
                  <FirstName>FirstName</FirstName>
                  <LastName>LastName</LastName>
                  <Suffix>Suffix</Suffix>
                </IndividualName>
                <BirthDate>2012-01-14</BirthDate>
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
        <DisclosureInformation>
          <ImplementingDate>2020-01-14</ImplementingDate>
          <Reason>DAC6704</Reason>
          <Summary>
            <Disclosure_Name>Name of Disclosure</Disclosure_Name>
            <Disclosure_Description>Description of disclosure</Disclosure_Description>
          </Summary>
          <NationalProvision>NationalProvision</NationalProvision>
          <Amount currCode="VUV">4000</Amount>
          <ConcernedMSs>
            <ConcernedMS>CZ</ConcernedMS>
          </ConcernedMSs>
          <MainBenefitTest1>false</MainBenefitTest1>
          <Hallmarks>
            <ListHallmarks>
              <Hallmark>DAC6A1</Hallmark>
            </ListHallmarks>
          </Hallmarks>
        </DisclosureInformation>
      </DAC6Disclosures>
    </DAC6_Arrangement>

  val invalidXml = <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
    <Header>
      <MessageRefId>GB0000000XXX</MessageRefId>
      <Timestamp>2020-05-14T17:10:00</Timestamp>
    </Header>
    <DAC6Disclosures>
      <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
      <Disclosing>
        <ID>
          <Individual>
            <IndividualName>
              <FirstName>John</FirstName>
              <LastName>Charles</LastName>
              <Suffix>Mr</Suffix>
            </IndividualName>
            <BirthDate>2007-01-14</BirthDate>
            <BirthPlace>Random Town</BirthPlace>
            <TIN issuedBy="GB">AA000000D</TIN>
            <Address>
              <Street></Street>
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
      <InitialDisclosureMA></InitialDisclosureMA>
      <RelevantTaxPayers>
        <RelevantTaxpayer>
          <ID>
            <Individual>
              <IndividualName>
                <FirstName>Larry</FirstName>
                <LastName>C</LastName>
                <Suffix>Mr</Suffix>
              </IndividualName>
              <BirthDate>2007-01-14</BirthDate>
              <BirthPlace>Birthplace</BirthPlace>
              <TIN issuedBy="GB">AA000000D</TIN>
              <Address>
                <Street>Street</Street>
                <BuildingIdentifier>1</BuildingIdentifier>
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
          <TaxpayerImplementingDate>2020-05-14</TaxpayerImplementingDate>
          <AssociatedEnterprises>
            <AssociatedEnterprise>
              <AssociatedEnterpriseID>
                <Individual>
                  <IndividualName>
                    <FirstName>Name</FirstName>
                    <LastName>C</LastName>
                    <Suffix>(Cat)</Suffix>
                  </IndividualName>
                  <BirthDate>2007-01-14</BirthDate>
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
        </RelevantTaxpayer>
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
              <BirthDate>2007-01-14</BirthDate>
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
      <AffectedPersons>
        <AffectedPerson>
          <AffectedPersonID>
            <Individual>
              <IndividualName>
                <FirstName>FirstName</FirstName>
                <LastName>LastName</LastName>
                <Suffix>Suffix</Suffix>
              </IndividualName>
              <BirthDate>2012-01-14</BirthDate>
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
      <DisclosureInformation>
        <ImplementingDate>2020-01-14</ImplementingDate>
        <Reason>DAC6704</Reason>
        <Summary>
          <Disclosure_Name>Name of Disclosure</Disclosure_Name>
          <Disclosure_Description>Description of disclosure</Disclosure_Description>
        </Summary>
        <NationalProvision>NationalProvision</NationalProvision>
        <Amount currCode="VUV">4000</Amount>
        <ConcernedMSs>
          <ConcernedMS>CZ</ConcernedMS>
        </ConcernedMSs>
        <MainBenefitTest1>false</MainBenefitTest1>
        <Hallmarks>
          <ListHallmarks>
            <Hallmark>DAC6A1</Hallmark>
          </ListHallmarks>
        </Hallmarks>
      </DisclosureInformation>
    </DAC6Disclosures>
  </DAC6_Arrangement>

}
