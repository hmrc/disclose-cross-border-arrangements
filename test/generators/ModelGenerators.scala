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

package generators

import java.time.LocalDateTime

import models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

trait ModelGenerators extends BaseGenerators with JavaTimeGenerators {

  implicit lazy val arbitrarySubmissionDetails: Arbitrary[SubmissionDetails] =
    Arbitrary {
      for {
        enrolmentID <- arbitrary[String]
        submissionTime <- arbitrary[LocalDateTime]
        fileName <- arbitrary[String]
        arrangementID <- Gen.option(arbitrary[String])
        disclosureID <- Gen.option(arbitrary[String])
        importInstruction <- Gen.oneOf("New", "Add", "Replace", "Delete")
        initialDisclosureMA <- Gen.oneOf(true, false)
      } yield
        SubmissionDetails(
          enrolmentID,
          submissionTime,
          fileName,
          arrangementID,
          disclosureID,
          importInstruction,
          initialDisclosureMA)
    }

  implicit val arbitraryReturnParameters: Arbitrary[RequestParameter] = Arbitrary {
    for {
      paramName <- arbitrary[String]
      paramValue <- arbitrary[String]
    } yield RequestParameter(paramName, paramValue)
  }

  implicit lazy val arbitraryDisplaySubscriptionForDACRequest: Arbitrary[DisplaySubscriptionForDACRequest] = {
    Arbitrary {
      for {
        idNumber <- stringsWithMaxLength(30)
        conversationID <- Gen.option(stringsWithMaxLength(36))
        receiptDate <- arbitrary[String]
        acknowledgementReference <- arbitrary[String]
        originatingSystem <- arbitrary[String]
        requestParameter <- Gen.option(Gen.listOf(arbitrary[RequestParameter]))
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

}
