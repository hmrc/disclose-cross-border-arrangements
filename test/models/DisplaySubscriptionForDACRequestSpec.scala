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

package models

import base.SpecBase
import play.api.libs.json.{JsObject, Json}

class DisplaySubscriptionForDACRequestSpec extends SpecBase {

  val requestCommon: RequestCommon =
    RequestCommon(
      regime = "DAC",
      receiptDate = "2020-09-23T16:12:11Z",
      acknowledgementReference = "Abc12345",
      originatingSystem = "MDTP",
      requestParameters = None
    )

  val requestDetail: RequestDetail = RequestDetail(IDType = "SAFE", IDNumber = "XE0001234567890")

  val displaySubscriptionForDACRequest: DisplaySubscriptionForDACRequest =
    DisplaySubscriptionForDACRequest(
      DisplaySubscriptionDetails(requestCommon = requestCommon, requestDetail = requestDetail)
    )

  "DisplaySubscriptionForDACRequest" - {

    "must deserialise DisplaySubscriptionForDACRequest" in {
      val jsonPayload: String =
        s"""
           |{
           |  "displaySubscriptionForDACRequest": {
           |    "requestCommon": {
           |      "regime": "DAC",
           |      "receiptDate": "2020-09-23T16:12:11Z",
           |      "acknowledgementReference": "Abc12345",
           |      "originatingSystem": "MDTP"
           |    },
           |    "requestDetail": {
           |      "IDType": "SAFE",
           |      "IDNumber": "XE0001234567890"
           |    }
           |  }
           |}""".stripMargin

      Json.parse(jsonPayload).validate[DisplaySubscriptionForDACRequest].get mustBe displaySubscriptionForDACRequest
    }

    "must serialise DisplaySubscriptionForDACRequest" in {
      val json: JsObject = Json.obj(
        "displaySubscriptionForDACRequest" -> Json.obj(
          "requestCommon" -> Json.obj(
            "regime" -> "DAC",
            "receiptDate" -> "2020-09-23T16:12:11Z",
            "acknowledgementReference" -> "Abc12345",
            "originatingSystem" -> "MDTP"
          ),
          "requestDetail" -> Json.obj(
            "IDType" -> "SAFE",
            "IDNumber" -> "XE0001234567890"
          )
        )
      )

      Json.toJson(displaySubscriptionForDACRequest) mustBe json
    }
  }
}
