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

package models

import base.SpecBase
import generators.ModelGenerators
import helpers.JsonFixtures.{updateDetailsJson, updateDetailsJsonNoSecondContact, updateDetailsPayload, updateDetailsPayloadNoSecondContact}
import models.subscription._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.alphaNumStr
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsBoolean, JsString, Json}

class UpdateSubscriptionForDACRequestSpec extends SpecBase with ModelGenerators with ScalaCheckPropertyChecks {

  val requestParameter = Seq(RequestParameter("Name", "Value"))

  val requestCommon: RequestCommonForUpdate = RequestCommonForUpdate(
    regime = "DAC",
    receiptDate = "2020-09-23T16:12:11Z",
    acknowledgementReference = "AB123c",
    originatingSystem = "MDTP",
    requestParameters = Some(requestParameter)
  )

  "UpdateSubscriptionForDACRequest" - {

    "must deserialise UpdateSubscriptionForDACRequest" in {
      forAll(arbitrary[UpdateSubscriptionForDACRequest], nonNumerics, nonNumerics, nonNumerics, alphaNumStr, nonNumerics) {
        (updateSubscriptionForDAC, firstName, lastName, orgName, phone, email) =>
          val requestDetail = updateSubscriptionForDAC.updateSubscriptionForDACRequest.requestDetail

          val primaryContact: PrimaryContact = PrimaryContact(
            Seq(ContactInformationForIndividual(IndividualDetails(firstName, lastName, None), email, None, None))
          )

          val secondaryContact = SecondaryContact(
            Seq(ContactInformationForOrganisation(OrganisationDetails(orgName), email, Some(phone), None))
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = requestDetail.IDNumber,
            tradingName = None,
            isGBUser =  requestDetail.isGBUser,
            primaryContact = primaryContact,
            secondaryContact = Some(secondaryContact)
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          val jsonPayload = updateDetailsPayload(JsString(requestDetail.IDNumber), JsBoolean(requestDetail.isGBUser),
            JsString(firstName), JsString(lastName), JsString(email), JsString(orgName), JsString(phone))

          Json.parse(jsonPayload).validate[UpdateSubscriptionForDACRequest].get mustBe updateRequest
      }
    }

    "must deserialise UpdateSubscriptionForDACRequest without secondary contact" in {
      forAll(arbitrary[UpdateSubscriptionForDACRequest], nonNumerics, nonNumerics, nonNumerics) {
        (updateSubscriptionForDAC, firstName, lastName, primaryEmail) =>
          val requestDetail = updateSubscriptionForDAC.updateSubscriptionForDACRequest.requestDetail

          val primaryContactForInd: PrimaryContact = PrimaryContact(
            Seq(ContactInformationForIndividual(IndividualDetails(firstName, lastName, None), primaryEmail, None, None))
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = requestDetail.IDNumber,
            tradingName = None,
            isGBUser =  requestDetail.isGBUser,
            primaryContact = primaryContactForInd,
            secondaryContact = None
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          val jsonPayload = updateDetailsPayloadNoSecondContact(JsString(requestDetail.IDNumber),
            JsBoolean(requestDetail.isGBUser), JsString(firstName), JsString(lastName), JsString(primaryEmail))

          Json.parse(jsonPayload).validate[UpdateSubscriptionForDACRequest].get mustBe updateRequest
      }
    }

    "must serialise UpdateSubscriptionForDACRequest" in {
      forAll(arbitrary[UpdateSubscriptionForDACRequest], nonNumerics, nonNumerics, nonNumerics, alphaNumStr, nonNumerics) {
        (updateSubscriptionForDAC, firstName, lastName, orgName, phone, email) =>
          val requestDetail = updateSubscriptionForDAC.updateSubscriptionForDACRequest.requestDetail

          val primaryContactForInd: PrimaryContact = PrimaryContact(
            Seq(ContactInformationForIndividual(IndividualDetails(firstName, lastName, None), email, None, None))
          )

          val secondaryContact = SecondaryContact(
            Seq(ContactInformationForOrganisation(OrganisationDetails(orgName), email, Some(phone), None))
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = requestDetail.IDNumber,
            tradingName = None,
            isGBUser =  requestDetail.isGBUser,
            primaryContact = primaryContactForInd,
            secondaryContact = Some(secondaryContact)
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          Json.toJson(updateRequest) mustBe updateDetailsJson(requestDetail.IDNumber, requestDetail.isGBUser,
            firstName, lastName, email, orgName, phone)
      }
    }

    "must serialise UpdateSubscriptionForDACRequest without secondary contact" in {
      forAll(arbitrary[UpdateSubscriptionForDACRequest], nonNumerics, nonNumerics, nonNumerics) {
        (updateSubscriptionForDAC, firstName, lastName, primaryEmail) =>
          val requestDetail = updateSubscriptionForDAC.updateSubscriptionForDACRequest.requestDetail

          val primaryContactForInd: PrimaryContact = PrimaryContact(
            Seq(ContactInformationForIndividual(IndividualDetails(firstName, lastName, None), primaryEmail, None, None))
          )

          val requestDetailForUpdate = RequestDetailForUpdate(
            IDType = "SAFE",
            IDNumber = requestDetail.IDNumber,
            tradingName = None,
            isGBUser =  requestDetail.isGBUser,
            primaryContact = primaryContactForInd,
            secondaryContact = None
          )

          val updateRequest = UpdateSubscriptionForDACRequest(
            UpdateSubscriptionDetails(
              requestCommon = requestCommon,
              requestDetail = requestDetailForUpdate
            )
          )

          Json.toJson(updateRequest) mustBe updateDetailsJsonNoSecondContact(requestDetail.IDNumber, requestDetail.isGBUser,
            firstName, lastName, primaryEmail)
      }
    }

  }

}
