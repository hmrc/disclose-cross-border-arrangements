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

import base.SpecBase
import models.{GenericError, Validation}

class ValidationSpec extends SpecBase with TestXml {

  "Validation" - {
    "getErrorMessage" - {

      "must correct error message when there should be a RelevantTaxpayer" in {

        val validator = new Validation("businessrules.initialDisclosure.needRelevantTaxPayer", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "InitialDisclosureMA is false so there should be a RelevantTaxpayer"))
      }

      "must correct error message when other info is provided when hallmark absent" in {

        val validator = new Validation("businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "DAC6D1OtherInfo has been provided but hallmark DAC6D1Other has not been selected"))
      }

      "must correct error message - RelevantTaxpayerDiscloser has been provided so there must be at least one RelevantTaxpayer" in {

        val validator = new Validation("businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "RelevantTaxpayerDiscloser has been provided so there must be at least one RelevantTaxpayer"))
      }

      "must correct error message - IntermediaryDiscloser has been provided so there must be at least one Intermediary" in {

        val validator = new Validation("businessrules.intermediaryDiscloser.needIntermediary", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "IntermediaryDiscloser has been provided so there must be at least one Intermediary"))
      }

      "must correct error message - Check the TaxpayerImplementingDate for all arrangements is on or after 25 June 2018" in {

        val validator = new Validation("businessrules.taxPayerImplementingDates.needToBeAfterStart", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Check the TaxpayerImplementingDate for all arrangements is on or after 25 June 2018"))
      }

      "must correct error message - InitialDisclosureMA is true and there are RelevantTaxpayers so each RelevantTaxpayer must have a TaxpayerImplementingDate" in {

        val validator = new Validation("businessrules.initialDisclosureMA.missingRelevantTaxPayerDates", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0,
                                    "InitialDisclosureMA is true and there are RelevantTaxpayers so each RelevantTaxpayer must have a TaxpayerImplementingDate"
        ))
      }

      "must correct error message - ArrangementID relates to a previous initial disclosure where InitialDisclosureMA is true so each RelevantTaxpayer must have a TaxpayerImplementingDate" in {

        val validator = new Validation("businessrules.initialDisclosureMA.firstDisclosureHasInitialDisclosureMAAsTrue", false)

        val result = validator.toGenericError
        result mustBe (
          GenericError(
            0,
            "ArrangementID relates to a previous initial disclosure where InitialDisclosureMA is true so each RelevantTaxpayer must have a TaxpayerImplementingDate"
          )
        )
      }

      "must correct error message - MainBenefitTest1 is false or blank but the hallmarks A, B, C1bi, C1c and/or C1d have been selected" in {

        val validator = new Validation("businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "MainBenefitTest1 is false or blank but the hallmarks A, B, C1bi, C1c and/or C1d have been selected"))
      }

      "must correct error message - The DisclosureInformation/ImplementingDate on which the first step in the implementation of the reportable cross-border arrangement has been made or will be made must be on or after 25 June 2018" in {

        val validator = new Validation("businessrules.implementingDates.needToBeAfterStart", false)

        val result = validator.toGenericError
        result mustBe (
          GenericError(
            0,
            "The DisclosureInformation/ImplementingDate on which the first step in the implementation of the reportable cross-border arrangement has been made or will be made must be on or after 25 June 2018"
          )
        )
      }

      "must correct error message - DisclosureImportInstruction is DAC6ADD so there should be an ArrangementID and no DisclosureID" in {

        val validator = new Validation("businessrules.addDisclosure.mustHaveArrangementIDButNotDisclosureID", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "DisclosureImportInstruction is DAC6ADD so there should be an ArrangementID and no DisclosureID"))
      }

      "must correct error message - DisclosureImportInstruction is DAC6NEW so there should be no ArrangementID or DisclosureID" in {

        val validator = new Validation("businessrules.newDisclosure.mustNotHaveArrangementIDOrDisclosureID", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "DisclosureImportInstruction is DAC6NEW so there should be no ArrangementID or DisclosureID"))
      }

      "must correct error message - DisclosureImportInstruction is DAC6REP so there should be an ArrangementID and a DisclosureID" in {

        val validator = new Validation("businessrules.repDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "DisclosureImportInstruction is DAC6REP so there should be an ArrangementID and a DisclosureID"))
      }

      "must correct error message - DisclosureImportInstruction is DAC6DEL so there should be an ArrangementID and a DisclosureID" in {

        val validator = new Validation("businessrules.delDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "DisclosureImportInstruction is DAC6DEL so there should be an ArrangementID and a DisclosureID"))
      }

      "must correct error message - InitialDisclosureMA is true so DisclosureImportInstruction cannot be DAC6ADD" in {

        val validator = new Validation("businessrules.addDisclosure.mustNotBeInitialDisclosureMA", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "InitialDisclosureMA is true so DisclosureImportInstruction cannot be DAC6ADD"))
      }

      "must correct error message - Remove the TaxpayerImplementingDate for any arrangements that are not marketable" in {

        val validator = new Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Remove the TaxpayerImplementingDate for any arrangements that are not marketable"))
      }

      "must correct error message - Check BirthDate field is on or after 1 January 1900 for all RelevantTaxPayers" in {

        val validator = new Validation("businessrules.RelevantTaxPayersBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Check BirthDate field is on or after 1 January 1900 for all RelevantTaxPayers"))
      }

      "must correct error message - Check BirthDate field is on or after 1 January 1900 for Disclosing" in {

        val validator = new Validation("businessrules.DisclosingBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Check BirthDate field is on or after 1 January 1900 for Disclosing"))
      }

      "must correct error message - Check BirthDate field is on or after 1 January 1900 for all intermediaries" in {

        val validator = new Validation("businessrules.IntermediaryBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Check BirthDate field is on or after 1 January 1900 for all intermediaries"))
      }

      "must correct error message - Check BirthDate field is on or after 1 January 1900 for all AffectedPersons" in {

        val validator = new Validation("businessrules.AffectedPersonsBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Check BirthDate field is on or after 1 January 1900 for all AffectedPersons"))
      }

      "must correct error message - Check BirthDate field is on or after 1 January 1900 for all AssociatedEnterprises" in {

        val validator = new Validation("businessrules.AssociatedEnterprisesBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Check BirthDate field is on or after 1 January 1900 for all AssociatedEnterprises"))
      }

      "MD -must provide correct error message - ArrangementID does not match HMRC's records" in {

        val validator = new Validation("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "ArrangementID does not match HMRC's records"))
      }

      "MD -must provide correct error message - Provide DisclosureInformation in this DAC6REP file, to replace the original arrangement details" in {

        val validator = new Validation("metaDataRules.disclosureInformation.noInfoWhenReplacingDAC6NEW", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Provide DisclosureInformation in this DAC6REP file, to replace the original arrangement details"))
      }

      "MD -must provide correct error message - Provide DisclosureInformation in this DAC6REP file. This is a mandatory field for arrangements that are not marketable" in {

        val validator = new Validation("metaDataRules.disclosureInformation.noInfoForNonMaDAC6REP", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0,
                                    "Provide DisclosureInformation in this DAC6REP file. This is a mandatory field for arrangements that are not marketable"
        ))
      }

      "MD -must provide correct error message - Change the InitialDisclosureMA to match the original declaration. If the arrangement has since become marketable, you will need to make a new report" in {

        val validator = new Validation("metaDataRules.initialDisclosureMA.arrangementNowMarketable", false)

        val result = validator.toGenericError
        result mustBe (
          GenericError(
            0,
            "Change the InitialDisclosureMA to match the original declaration. If the arrangement has since become marketable, you will need to make a new report"
          )
        )
      }

      "MD -must provide correct error message - Change the InitialDisclosureMA to match the original declaration. If the arrangement is no longer marketable, you will need to make a new report" in {

        val validator = new Validation("metaDataRules.initialDisclosureMA.arrangementNoLongerMarketable", false)

        val result = validator.toGenericError
        result mustBe (
          GenericError(
            0,
            "Change the InitialDisclosureMA to match the original declaration. If the arrangement is no longer marketable, you will need to make a new report"
          )
        )
      }

      "MD -must provide correct error message - DisclosureID does not match the ArrangementID provided" in {

        val validator = new Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchArrangementID", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "DisclosureID does not match the ArrangementID provided"))
      }

      "MD -must provide correct error message - DisclosureID has not been generated by this individual or organisation" in {

        val validator = new Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchUser", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "DisclosureID has not been generated by this individual or organisation"))
      }

      "MD -must provide correct error message - Provide DisclosureInformation in this DAC6NEW file" in {

        val validator = new Validation("metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6NEW", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Provide DisclosureInformation in this DAC6NEW file"))
      }

      "MD -must provide correct error message - Provide DisclosureInformation in this DAC6ADD file. This is a mandatory field for arrangements that are not marketable" in {

        val validator = new Validation("metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6ADD", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0,
                                    "Provide DisclosureInformation in this DAC6ADD file. This is a mandatory field for arrangements that are not marketable"
        ))
      }
      "MD -must provide correct error message - The MessageRefID should start with GB, then your User ID, followed by identifying characters of your choice. It must be 200 characters or less" in {

        val validator = new Validation("metaDataRules.messageRefId.wrongFormat", false)

        val result = validator.toGenericError
        result mustBe (
          GenericError(
            0,
            "The MessageRefID should start with GB, then your User ID, followed by identifying characters of your choice. It must be 200 characters or less"
          )
        )
      }

      "MD -must provide correct error message - Check UserID is correct, it must match the ID you got at registration to create a valid MessageRefID" in {

        val validator = new Validation("metaDataRules.messageRefId.noUserId", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "Check UserID is correct, it must match the ID you got at registration to create a valid MessageRefID"))
      }

      "MD -must provide correct error message - Check your MessageRefID is unique. It should start with GB, then your User ID, followed by unique identifying characters of your choice. It must be 200 characters or less" in {

        val validator = new Validation("metaDataRules.messageRefId.notUnique", false)

        val result = validator.toGenericError
        result mustBe (
          GenericError(
            0,
            "Check your MessageRefID is unique. It should start with GB, then your User ID, followed by unique identifying characters of your choice. It must be 200 characters or less"
          )
        )
      }

      "must provide correct error message - No Key supplied" in {

        val validator = new Validation("None", false)

        val result = validator.toGenericError
        result mustBe (GenericError(0, "There is a problem with this line number"))
      }

      "key -must provide correct error message - InitialDisclosureMA needRelevantTaxPayer" in {

        val validator = new Validation("businessrules.initialDisclosure.needRelevantTaxPayer", false)

        val result = validator.path
        result mustBe "InitialDisclosureMA"
      }

      "key -must provide correct error message - RelevantTaxpayerDiscloser needRelevantTaxPayer" in {

        val validator = new Validation("businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer", false)

        val result = validator.path
        result mustBe "RelevantTaxpayerDiscloser"
      }

      "key -must provide correct error message - IntermediaryDiscloser needIntermediary" in {

        val validator = new Validation("businessrules.intermediaryDiscloser.needIntermediary", false)

        val result = validator.path
        result mustBe "IntermediaryDiscloser"
      }

      "key -must provide correct error message - RelevantTaxPayers needToBeAfterStart" in {

        val validator = new Validation("businessrules.taxPayerImplementingDates.needToBeAfterStart", false)

        val result = validator.path
        result mustBe "RelevantTaxPayers"
      }

      "key -must provide correct error message - ImplementingDate needToBeAfterStart" in {

        val validator = new Validation("businessrules.implementingDates.needToBeAfterStart", false)

        val result = validator.path
        result mustBe "ImplementingDate"
      }

      "key -must provide correct error message - RelevantTaxPayers missingRelevantTaxPayerDates" in {

        val validator = new Validation("businessrules.initialDisclosureMA.missingRelevantTaxPayerDates", false)

        val result = validator.path
        result mustBe "RelevantTaxPayers"
      }

      "key -must provide correct error message - InitialDisclosureMA firstDisclosureHasInitialDisclosureMAAsTrue" in {

        val validator = new Validation("businessrules.initialDisclosureMA.firstDisclosureHasInitialDisclosureMAAsTrue", false)

        val result = validator.path
        result mustBe "InitialDisclosureMA"
      }

      "key -must provide correct error message - Hallmarks oneOfSpecificHallmarksMustBePresent" in {

        val validator = new Validation("businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent", false)

        val result = validator.path
        result mustBe "Hallmarks"
      }

      "key -must provide correct error message - DAC6D1OtherInfo needHallMarkToProvideInfo" in {

        val validator = new Validation("businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo", false)

        val result = validator.path
        result mustBe "DAC6D1OtherInfo"
      }

      "key -must provide correct error message - RelevantTaxPayers cantHaveRelevantTaxPayer" in {

        val validator = new Validation("businessrules.nonMA.cantHaveRelevantTaxPayer", false)

        val result = validator.path
        result mustBe "RelevantTaxPayers"
      }

      "key -must provide correct error message - RelevantTaxPayers maxDateOfBirthExceeded" in {

        val validator = new Validation("businessrules.RelevantTaxPayersBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.path
        result mustBe "RelevantTaxPayers"
      }

      "key -must provide correct error message - Disclosing maxDateOfBirthExceeded" in {

        val validator = new Validation("businessrules.DisclosingBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.path
        result mustBe "Disclosing"
      }

      "key -must provide correct error message - Intermediaries maxDateOfBirthExceeded" in {

        val validator = new Validation("businessrules.IntermediaryBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.path
        result mustBe "Intermediaries"
      }

      "key -must provide correct error message - AffectedPersons maxDateOfBirthExceeded" in {

        val validator = new Validation("businessrules.AffectedPersonsBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.path
        result mustBe "AffectedPersons"
      }

      "key -must provide correct error message - AssociatedEnterprises maxDateOfBirthExceeded" in {

        val validator = new Validation("businessrules.AssociatedEnterprisesBirthDates.maxDateOfBirthExceeded", false)

        val result = validator.path
        result mustBe "AssociatedEnterprises"
      }

      "key -must provide correct error message - ArrangementID maxDateOfBirthExceeded" in {

        val validator = new Validation("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords", false)

        val result = validator.path
        result mustBe "ArrangementID"
      }

      "key -must provide correct error message - DisclosureImportInstruction noInfoWhenReplacingDAC6NEW" in {

        val validator = new Validation("metaDataRules.disclosureInformation.noInfoWhenReplacingDAC6NEW", false)

        val result = validator.path
        result mustBe "DisclosureImportInstruction"
      }

      "key -must provide correct error message - DisclosureImportInstruction noInfoForNonMaDAC6REP" in {

        val validator = new Validation("metaDataRules.disclosureInformation.noInfoForNonMaDAC6REP", false)

        val result = validator.path
        result mustBe "DisclosureImportInstruction"
      }

      "key -must provide correct error message - InitialDisclosureMA arrangementNowMarketable" in {

        val validator = new Validation("metaDataRules.initialDisclosureMA.arrangementNowMarketable", false)

        val result = validator.path
        result mustBe "InitialDisclosureMA"
      }

      "key -must provide correct error message - InitialDisclosureMA arrangementNoLongerMarketable" in {

        val validator = new Validation("metaDataRules.initialDisclosureMA.arrangementNoLongerMarketable", false)

        val result = validator.path
        result mustBe "InitialDisclosureMA"
      }

      "key -must provide correct error message - DisclosureID arrangementNoLongerMarketable" in {

        val validator = new Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchArrangementID", false)

        val result = validator.path
        result mustBe "DisclosureID"
      }

      "key -must provide correct error message - DisclosureID disclosureIDDoesNotMatchUser" in {

        val validator = new Validation("metaDataRules.disclosureId.disclosureIDDoesNotMatchUser", false)

        val result = validator.path
        result mustBe "DisclosureID"
      }

      "key -must provide correct error message - DisclosureImportInstruction disclosureInformationMissingFromDAC6NEW" in {

        val validator = new Validation("metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6NEW", false)

        val result = validator.path
        result mustBe "DisclosureImportInstruction"
      }

      "key -must provide correct error message - DisclosureImportInstruction disclosureInformationMissingFromDAC6ADD" in {

        val validator = new Validation("metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6ADD", false)

        val result = validator.path
        result mustBe "DisclosureImportInstruction"
      }

      "key -must provide correct error message - MessageRefId wrongFormat" in {

        val validator = new Validation("metaDataRules.messageRefId.wrongFormat", false)

        val result = validator.path
        result mustBe "MessageRefId"
      }

      "key -must provide correct error message - MessageRefId noUserId" in {

        val validator = new Validation("metaDataRules.messageRefId.noUserId", false)

        val result = validator.path
        result mustBe "MessageRefId"
      }

      "key -must provide correct error message - MessageRefId notUnique" in {

        val validator = new Validation("metaDataRules.messageRefId.notUnique", false)

        val result = validator.path
        result mustBe "MessageRefId"
      }

      "key -must provide correct error message - InitialDisclosureMA notUnique" in {

        val validator = new Validation("businessrules.addDisclosure.mustNotBeInitialDisclosureMA", false)

        val result = validator.path
        result mustBe "InitialDisclosureMA"
      }

      "key -must provide correct error message - no matching key" in {

        val validator = new Validation("None", false)

        val result = validator.path
        result mustBe "DisclosureImportInstruction"
      }
    }
  }
}
