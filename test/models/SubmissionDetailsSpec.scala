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
import controllers.APIDateTimeFormats
import play.api.libs.json.{JsObject, Json}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SubmissionDetailsSpec extends SpecBase {

  val submissionTime = LocalDateTime.now

  val submissionDetails = SubmissionDetails(
    enrolmentID = "enrolmentID",
    submissionTime = submissionTime,
    fileName = "fileName",
    arrangementID = Some("arrangementID"),
    disclosureID = Some("disclosureID"),
    importInstruction = "New",
    initialDisclosureMA = false,
    messageRefId = "messageRefId"
  )

  "SubmissionDetails" - {

    "must serialise SubmissionDetails in API calls" in {

      val json: JsObject = Json.obj(
        "enrolmentID"         -> "enrolmentID",
        "submissionTime"      -> submissionTime.format(DateTimeFormatter.ISO_DATE_TIME),
        "fileName"            -> "fileName",
        "arrangementID"       -> "arrangementID",
        "disclosureID"        -> "disclosureID",
        "importInstruction"   -> "New",
        "initialDisclosureMA" -> false,
        "messageRefId"        -> "messageRefId"
      )

      import APIDateTimeFormats._
      Json.toJson(submissionDetails).toString mustEqual json.toString
    }
  }
}
