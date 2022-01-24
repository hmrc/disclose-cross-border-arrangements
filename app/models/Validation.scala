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

package models

case class Validation(key: String, value: Boolean, lineNumber: Option[Int] = None) {

  def toGenericError: GenericError = GenericError(lineNumber.getOrElse(0), getErrorMessage)

  def setLineNumber(xmlArray: Array[String]): Validation = {
    val index = xmlArray.indexWhere(
      str => str.contains(path)
    ) + 1
    copy(lineNumber = Some(index))
  }

  def getErrorMessage: String = {
    key match {
      case "businessrules.initialDisclosure.needRelevantTaxPayer"     => "InitialDisclosureMA is false so there should be a RelevantTaxpayer"
      case "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo" => "DAC6D1OtherInfo has been provided but hallmark DAC6D1Other has not been selected"
      case "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer" =>
        "RelevantTaxpayerDiscloser has been provided so there must be at least one RelevantTaxpayer"
      case "businessrules.intermediaryDiscloser.needIntermediary"       => "IntermediaryDiscloser has been provided so there must be at least one Intermediary"
      case "businessrules.taxPayerImplementingDates.needToBeAfterStart" => "Check the TaxpayerImplementingDate for all arrangements is on or after 25 June 2018"
      case "businessrules.initialDisclosureMA.missingRelevantTaxPayerDates" =>
        "InitialDisclosureMA is true and there are RelevantTaxpayers so each RelevantTaxpayer must have a TaxpayerImplementingDate"
      case "businessrules.initialDisclosureMA.firstDisclosureHasInitialDisclosureMAAsTrue" =>
        "ArrangementID relates to a previous initial disclosure where InitialDisclosureMA is true so each RelevantTaxpayer must have a TaxpayerImplementingDate"
      case "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent" =>
        "MainBenefitTest1 is false or blank but the hallmarks A, B, C1bi, C1c and/or C1d have been selected"
      case "businessrules.implementingDates.needToBeAfterStart" =>
        "The DisclosureInformation/ImplementingDate on which the first step in the implementation of the reportable cross-border arrangement has been made or will be made must be on or after 25 June 2018"
      case "businessrules.addDisclosure.mustHaveArrangementIDButNotDisclosureID" =>
        "DisclosureImportInstruction is DAC6ADD so there should be an ArrangementID and no DisclosureID"
      case "businessrules.newDisclosure.mustNotHaveArrangementIDOrDisclosureID" =>
        "DisclosureImportInstruction is DAC6NEW so there should be no ArrangementID or DisclosureID"
      case "businessrules.repDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID" =>
        "DisclosureImportInstruction is DAC6REP so there should be an ArrangementID and a DisclosureID"
      case "businessrules.delDisclosure.mustHaveArrangementIDDisclosureIDAndMessageRefID" =>
        "DisclosureImportInstruction is DAC6DEL so there should be an ArrangementID and a DisclosureID"
      case "businessrules.addDisclosure.mustNotBeInitialDisclosureMA"         => "InitialDisclosureMA is true so DisclosureImportInstruction cannot be DAC6ADD"
      case "businessrules.nonMA.cantHaveRelevantTaxPayer"                     => "Remove the TaxpayerImplementingDate for any arrangements that are not marketable"
      case "businessrules.RelevantTaxPayersBirthDates.maxDateOfBirthExceeded" => "Check BirthDate field is on or after 1 January 1900 for all RelevantTaxPayers"
      case "businessrules.DisclosingBirthDates.maxDateOfBirthExceeded"        => "Check BirthDate field is on or after 1 January 1900 for Disclosing"
      case "businessrules.IntermediaryBirthDates.maxDateOfBirthExceeded"      => "Check BirthDate field is on or after 1 January 1900 for all intermediaries"
      case "businessrules.AffectedPersonsBirthDates.maxDateOfBirthExceeded"   => "Check BirthDate field is on or after 1 January 1900 for all AffectedPersons"
      case "businessrules.AssociatedEnterprisesBirthDates.maxDateOfBirthExceeded" =>
        "Check BirthDate field is on or after 1 January 1900 for all AssociatedEnterprises"
      case "businessrules.hallmarks.dHallmarkNotProvided"        => "Enter a category D hallmark only"
      case "businessrules.hallmarks.dHallmarkWithOtherHallmarks" => "Enter a category D hallmark only"

      case "metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords" => "ArrangementID does not match HMRC's records"
      case "metaDataRules.disclosureInformation.noInfoOnWhenInitialDisclosureWasFalseForDAC6DEL" =>
        "Provide DisclosureInformation in this DAC6DEL file. This is mandatory for arrangements that are not marketable"
      case "metaDataRules.disclosureInformation.noInfoWhenReplacingDAC6NEW" =>
        "Provide DisclosureInformation in this DAC6REP file, to replace the original arrangement details"
      case "metaDataRules.disclosureInformation.noInfoForNonMaDAC6REP" =>
        "Provide DisclosureInformation in this DAC6REP file. This is a mandatory field for arrangements that are not marketable"
      case "metaDataRules.initialDisclosureMA.arrangementNowMarketable" =>
        "Change the InitialDisclosureMA to match the original declaration. If the arrangement has since become marketable, you will need to make a new report"
      case "metaDataRules.initialDisclosureMA.arrangementNoLongerMarketable" =>
        "Change the InitialDisclosureMA to match the original declaration. If the arrangement is no longer marketable, you will need to make a new report"
      case "metaDataRules.disclosureId.disclosureIDDoesNotMatchArrangementID"            => "DisclosureID does not match the ArrangementID provided"
      case "metaDataRules.disclosureId.disclosureIDDoesNotMatchUser"                     => "DisclosureID has not been generated by this individual or organisation"
      case "metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6NEW" => "Provide DisclosureInformation in this DAC6NEW file"
      case "metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6ADD" =>
        "Provide DisclosureInformation in this DAC6ADD file. This is a mandatory field for arrangements that are not marketable"
      case "metaDataRules.messageRefId.wrongFormat" =>
        "The MessageRefID should start with GB, then your User ID, followed by identifying characters of your choice. It must be 200 characters or less"
      case "metaDataRules.messageRefId.noUserId" => "Check UserID is correct, it must match the ID you got at registration to create a valid MessageRefID"
      case "metaDataRules.messageRefId.notUnique" =>
        "Check your MessageRefID is unique. It should start with GB, then your User ID, followed by unique identifying characters of your choice. It must be 200 characters or less"

      case _ => "There is a problem with this line number"

    }
  }

  def path: String = {

    key match {
      case "businessrules.initialDisclosure.needRelevantTaxPayer"                          => "InitialDisclosureMA"
      case "businessrules.relevantTaxpayerDiscloser.needRelevantTaxPayer"                  => "RelevantTaxpayerDiscloser"
      case "businessrules.intermediaryDiscloser.needIntermediary"                          => "IntermediaryDiscloser"
      case "businessrules.taxPayerImplementingDates.needToBeAfterStart"                    => "RelevantTaxPayers"
      case "businessrules.implementingDates.needToBeAfterStart"                            => "ImplementingDate"
      case "businessrules.initialDisclosureMA.missingRelevantTaxPayerDates"                => "RelevantTaxPayers"
      case "businessrules.initialDisclosureMA.firstDisclosureHasInitialDisclosureMAAsTrue" => "InitialDisclosureMA"
      case "businessrules.mainBenefitTest1.oneOfSpecificHallmarksMustBePresent"            => "Hallmarks"
      case "businessrules.dac6D10OtherInfo.needHallMarkToProvideInfo"                      => "DAC6D1OtherInfo"
      case "businessrules.nonMA.cantHaveRelevantTaxPayer"                                  => "RelevantTaxPayers"
      case "businessrules.RelevantTaxPayersBirthDates.maxDateOfBirthExceeded"              => "RelevantTaxPayers"
      case "businessrules.DisclosingBirthDates.maxDateOfBirthExceeded"                     => "Disclosing"
      case "businessrules.IntermediaryBirthDates.maxDateOfBirthExceeded"                   => "Intermediaries"
      case "businessrules.AffectedPersonsBirthDates.maxDateOfBirthExceeded"                => "AffectedPersons"
      case "businessrules.AssociatedEnterprisesBirthDates.maxDateOfBirthExceeded"          => "AssociatedEnterprises"
      case "businessrules.hallmarks.dHallmarkNotProvided"                                  => "Hallmarks"
      case "businessrules.hallmarks.dHallmarkWithOtherHallmarks"                           => "Hallmarks"

      case "metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords"                        => "ArrangementID"
      case "metaDataRules.disclosureInformation.noInfoOnWhenInitialDisclosureWasFalseForDAC6DEL" => "DisclosureImportInstruction"
      case "metaDataRules.disclosureInformation.noInfoWhenReplacingDAC6NEW"                      => "DisclosureImportInstruction"
      case "metaDataRules.disclosureInformation.noInfoForNonMaDAC6REP"                           => "DisclosureImportInstruction"
      case "metaDataRules.initialDisclosureMA.arrangementNowMarketable"                          => "InitialDisclosureMA"
      case "metaDataRules.initialDisclosureMA.arrangementNoLongerMarketable"                     => "InitialDisclosureMA"
      case "metaDataRules.disclosureId.disclosureIDDoesNotMatchArrangementID"                    => "DisclosureID"
      case "metaDataRules.disclosureId.disclosureIDDoesNotMatchUser"                             => "DisclosureID"
      case "metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6NEW"         => "DisclosureImportInstruction"
      case "metaDataRules.disclosureInformation.disclosureInformationMissingFromDAC6ADD"         => "DisclosureImportInstruction"
      case "metaDataRules.messageRefId.wrongFormat"                                              => "MessageRefId"
      case "metaDataRules.messageRefId.noUserId"                                                 => "MessageRefId"
      case "metaDataRules.messageRefId.notUnique"                                                => "MessageRefId"
      case "businessrules.addDisclosure.mustNotBeInitialDisclosureMA"                            => "InitialDisclosureMA"

      case _ => "DisclosureImportInstruction"

    }
  }
}
