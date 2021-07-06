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
import play.api.libs.json.Json

class ErrorDetailsSpec extends SpecBase {

  "Error Details" - {
    "must be constructed from a BadRequest" in {
      val json = """
                   |{"errorDetail": {
                   |  "timestamp" : "2017-02-14T12:58:44Z",
                   |  "correlationId": "aaaa",
                   |  "errorCode": "400",
                   |  "errorMessage": "Invalid ID",
                   |  "source": "Back End",
                   |  "sourceFaultDetail":{
                   |    "detail":[
                   |      "001 - Regime missing or invalid"
                   |    ]}
                   |}}""".stripMargin

      val model = ErrorDetails(
        ErrorDetail(
          timestamp = "2017-02-14T12:58:44Z",
          correlationId = "aaaa",
          errorCode = "400",
          errorMessage = "Invalid ID",
          source = "Back End",
          sourceFaultDetail = Some(
            SourceFaultDetail(
              Seq("001 - Regime missing or invalid")
            )
          )
        )
      )

      Json.parse(json).validate[ErrorDetails].get mustBe model
    }

    "must be constructed from an Internal Server Error" in {
      val json =
        """{
          |  "errorDetail": {
          |    "timestamp": "2016-08-16T18:15:41Z",
          |    "correlationId": "aaaa",
          |    "errorCode": "500",
          |    "errorMessage": "Internal error",
          |    "source": "Internal error"
          |  }
          |}""".stripMargin

      val model = ErrorDetails(
        ErrorDetail(
          timestamp = "2016-08-16T18:15:41Z",
          correlationId = "aaaa",
          errorCode = "500",
          errorMessage = "Internal error",
          source = "Internal error",
          sourceFaultDetail = None
        )
      )

      Json.parse(json).as[ErrorDetails] mustBe model
    }

    "must be constructed from a ServiceUnavailable" in {
      val json =
        """{
          |  "errorDetail": {
          |    "timestamp": "2016-08-16T18:15:41Z",
          |    "correlationId": "aaaa",
          |    "errorCode": "503",
          |    "errorMessage": "Send timeout",
          |    "source": "Back End",
          |    "sourceFaultDetail": {
          |      "detail": ["101504 - Send timeout"]
          |    }
          |  }
          |}""".stripMargin

      val model = ErrorDetails(
        ErrorDetail(
          timestamp = "2016-08-16T18:15:41Z",
          correlationId = "aaaa",
          errorCode = "503",
          errorMessage = "Send timeout",
          source = "Back End",
          sourceFaultDetail = Some(
            SourceFaultDetail(
              Seq("101504 - Send timeout")
            )
          )
        )
      )

      Json.parse(json).as[ErrorDetails] mustBe model
    }
  }

}
