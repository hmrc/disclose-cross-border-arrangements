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

import models.subscription.{DisplaySubscriptionDetails, DisplaySubscriptionForDACRequest}
import models.subscription.cache._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

trait CacheModelGenerators extends BaseGenerators with JavaTimeGenerators {

  implicit val arbitrarySubscriptionForDACRequest: Arbitrary[SubscriptionForDACRequest] = Arbitrary {
    for {
      requestCommon <-  arbitrary[RequestCommonForSubscription]
      requestDetail <-  arbitrary[RequestDetail]
    } yield SubscriptionForDACRequest(requestCommon, requestDetail)
  }

  implicit val arbitraryCreateSubscriptionForDACRequest: Arbitrary[CreateSubscriptionForDACRequest] = Arbitrary {
    for {
      subscription <- arbitrary[SubscriptionForDACRequest]
      subscriptionID <- arbitrary[String]
    } yield CreateSubscriptionForDACRequest(subscription, subscriptionID)
  }

  implicit val arbitraryRequestDetail: Arbitrary[RequestDetail] = Arbitrary {
    for {
      idType <-  arbitrary[String]
      idNumber <-  arbitrary[String]
      tradingName <-  arbitrary[Option[String]]
      isGBUser <-  arbitrary[Boolean]
      primaryContact <-  arbitrary[PrimaryContact]
      secondaryContact <-  arbitrary[Option[SecondaryContact]]
    } yield RequestDetail(idType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryOrganisationDetails: Arbitrary[OrganisationDetails] = Arbitrary {
    for {
      organisationName <- arbitrary[String]
    } yield OrganisationDetails(organisationName)
  }

  implicit val arbitraryContactInformationForOrganisation: Arbitrary[ContactInformationForOrganisation] = Arbitrary {
    for {
      organisation <-  arbitrary[OrganisationDetails]
      email <-  arbitrary[String]
      phone <-  arbitrary[Option[String]]
      mobile <-  arbitrary[Option[String]]
    } yield ContactInformationForOrganisation(organisation, email, phone, mobile)
  }

  implicit val arbitraryPrimaryContact: Arbitrary[PrimaryContact] = Arbitrary {
    for {
      contactInformation <- Gen.oneOf(arbitrary[ContactInformationForIndividual], arbitrary[ContactInformationForOrganisation])
    } yield PrimaryContact(contactInformation)
  }

  implicit val arbitrarySecondaryContact: Arbitrary[SecondaryContact] = Arbitrary {
    for {
      contactInformation <- Gen.oneOf(arbitrary[ContactInformationForIndividual], arbitrary[ContactInformationForOrganisation])
    } yield SecondaryContact(contactInformation)
  }

  implicit val arbitraryIndividualDetails: Arbitrary[IndividualDetails] = Arbitrary {
    for {
      firstName <- arbitrary[String]
      middleName <- arbitrary[Option[String]]
      lastName <- arbitrary[String]
    } yield IndividualDetails(firstName, middleName, lastName)
  }

  implicit val arbitraryContactInformationForIndividual : Arbitrary[ContactInformationForIndividual] = Arbitrary {
    for {
      individual <-  arbitrary[IndividualDetails]
      email <-  arbitrary[String]
      phone <-  arbitrary[Option[String]]
      mobile <-  arbitrary[Option[String]]
    } yield ContactInformationForIndividual(individual, email, phone, mobile)
  }

  implicit val arbitraryRequestParameter: Arbitrary[RequestParameter] = Arbitrary {
    for {
      paramName <- arbitrary[String]
      paramValue <- arbitrary[String]
    } yield RequestParameter(paramName, paramValue)
  }


  implicit val arbitraryRequestCommonForSubscription: Arbitrary[RequestCommonForSubscription] = Arbitrary {
    for {
      regime <- arbitrary[String]
      receiptDate <- arbitrary[String]
      acknowledgementReference <- arbitrary[String]
      originatingSystem <- arbitrary[String]
      requestParameters <- arbitrary[Option[Seq[RequestParameter]]]
    } yield RequestCommonForSubscription(regime, receiptDate, acknowledgementReference, originatingSystem, requestParameters)
  }

  implicit lazy val arbitraryDisplaySubscriptionForDACRequest: Arbitrary[DisplaySubscriptionForDACRequest] = {
    Arbitrary {
      for {
        idNumber <- stringsWithMaxLength(30)
        conversationID <- Gen.option(stringsWithMaxLength(36))
        receiptDate <- arbitrary[String]
        acknowledgementReference <- arbitrary[String]
        originatingSystem <- arbitrary[String]
      } yield {
        DisplaySubscriptionForDACRequest(
          DisplaySubscriptionDetails(
            requestCommon = models.subscription.RequestCommon(
              regime = "DAC",
              conversationID = conversationID,
              receiptDate = receiptDate,
              acknowledgementReference = acknowledgementReference,
              originatingSystem = originatingSystem,
              requestParameters = None
            ),
            requestDetail = models.subscription.RequestDetail(
              IDType = "DAC",
              IDNumber = idNumber
            )
          )
        )
      }
    }
  }
}
