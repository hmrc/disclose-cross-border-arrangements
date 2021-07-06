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

package generators

import java.time.LocalDateTime
import models._
import models.subscription._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.alphaStr
import org.scalacheck.{Arbitrary, Gen}

trait ModelGenerators extends BaseGenerators with JavaTimeGenerators {

  implicit lazy val arbitrarySubmissionDetails: Arbitrary[SubmissionDetails] =
    Arbitrary {
      for {
        enrolmentID         <- arbitrary[String]
        submissionTime      <- arbitrary[LocalDateTime]
        fileName            <- arbitrary[String]
        arrangementID       <- Gen.option(arbitrary[String])
        disclosureID        <- Gen.option(arbitrary[String])
        importInstruction   <- Gen.oneOf("New", "Add", "Replace", "Delete")
        initialDisclosureMA <- Gen.oneOf(true, false)
        messageRefId        <- arbitrary[String]
      } yield SubmissionDetails(enrolmentID, submissionTime, fileName, arrangementID, disclosureID, importInstruction, initialDisclosureMA, messageRefId)
    }

  implicit val arbitraryReturnParameters: Arbitrary[RequestParameter] = Arbitrary {
    for {
      paramName  <- alphaStr
      paramValue <- alphaStr
    } yield RequestParameter(paramName, paramValue)
  }

  implicit lazy val arbitraryDisplaySubscriptionForDACRequest: Arbitrary[DisplaySubscriptionForDACRequest] = {
    Arbitrary {
      for {
        idNumber                 <- stringsWithMaxLength(30)
        conversationID           <- Gen.option(stringsWithMaxLength(36))
        receiptDate              <- arbitrary[String]
        acknowledgementReference <- arbitrary[String]
        originatingSystem        <- arbitrary[String]
        requestParameter         <- Gen.option(Gen.listOf(arbitrary[RequestParameter]))
      } yield {
        DisplaySubscriptionForDACRequest(
          DisplaySubscriptionDetails(
            requestCommon = RequestCommon(
              regime = "DAC",
              conversationID = conversationID,
              receiptDate = receiptDate,
              acknowledgementReference = acknowledgementReference,
              originatingSystem = originatingSystem,
              requestParameters = requestParameter
            ),
            requestDetail = RequestDetail(
              IDType = "DAC",
              IDNumber = idNumber
            )
          )
        )
      }
    }
  }

  implicit val arbitraryContactInformationForIndividual: Arbitrary[ContactInformationForIndividual] = Arbitrary {
    for {
      firstName  <- alphaStr
      lastName   <- alphaStr
      middleName <- Gen.option(alphaStr)
      email      <- alphaStr
      telephone  <- Gen.option(alphaStr)
      mobile     <- Gen.option(alphaStr)
    } yield ContactInformationForIndividual(IndividualDetails(firstName, lastName, middleName), email, telephone, mobile)
  }

  implicit val arbitraryContactInformationForOrganisation: Arbitrary[ContactInformationForOrganisation] = Arbitrary {
    for {
      name      <- alphaStr
      email     <- alphaStr
      telephone <- Gen.option(alphaStr)
      mobile    <- Gen.option(alphaStr)
    } yield ContactInformationForOrganisation(OrganisationDetails(name), email, telephone, mobile)
  }

  implicit val arbitrarySecondaryContact: Arbitrary[SecondaryContact] = Arbitrary {
    for {
      contactInformation <- arbitrary[ContactInformationForOrganisation]
    } yield SecondaryContact(Seq(contactInformation))
  }

  implicit lazy val arbitraryUpdateSubscriptionForDACRequest: Arbitrary[UpdateSubscriptionForDACRequest] = {
    Arbitrary {
      for {
        idNumber                 <- stringsWithMaxLength(30)
        receiptDate              <- alphaStr
        acknowledgementReference <- alphaStr
        originatingSystem        <- alphaStr
        requestParameter         <- Gen.option(Gen.listOf(arbitrary[RequestParameter]))
        isGBUser                 <- arbitrary[Boolean]
        primaryContact           <- Gen.oneOf(arbitrary[ContactInformationForIndividual], arbitrary[ContactInformationForOrganisation])
        secondaryContact         <- Gen.option(arbitrary[SecondaryContact])
      } yield {
        UpdateSubscriptionForDACRequest(
          UpdateSubscriptionDetails(
            requestCommon = RequestCommonForUpdate(
              regime = "DAC",
              receiptDate = receiptDate,
              acknowledgementReference = acknowledgementReference,
              originatingSystem = originatingSystem,
              requestParameters = requestParameter
            ),
            requestDetail = RequestDetailForUpdate(
              IDType = "SAFE",
              IDNumber = idNumber,
              tradingName = None,
              isGBUser = isGBUser,
              primaryContact = PrimaryContact(Seq(primaryContact)),
              secondaryContact = secondaryContact
            )
          )
        )
      }
    }
  }

}
