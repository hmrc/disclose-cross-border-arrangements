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

package models.upscan

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, Json}

class UploadStatusSpec extends AnyWordSpec with Matchers {

  val statuses: List[UploadStatus] = List(NotStarted, Failed, InProgress, Quarantined)

  "UploadStatus json Reads" must {

    statuses.foreach {
      status =>
        s"return $status" when {
          s"_type is $status" in {
            val json = s"""{"_type": "$status"}"""
            Json.parse(json).as[UploadStatus] mustBe status
          }
        }
    }

    "return UploadedSuccessfully" when {
      "_type is UploadedSuccessfully" in {
        val expectedName     = "fileName"
        val expectedUrl      = "downloadUrl"
        val json             = s"""{"_type": "UploadedSuccessfully", "name": "$expectedName", "downloadUrl": "$expectedUrl"}"""
        val expectedResponse = UploadedSuccessfully(expectedName, expectedUrl)
        Json.parse(json).as[UploadStatus] mustBe expectedResponse
      }
    }

    "return JsonError" when {
      "_type is unexpected value" in {
        val unexpectedValue = "RandomValue"
        val json            = s"""{"_type": "$unexpectedValue"}"""
        Json.parse(json).validate[UploadStatus] mustBe JsError(s"""Unexpected value of _type: "$unexpectedValue"""")
      }

      "_type is missing from JSON" in {
        val json = """{"type": "RandomValue"}"""
        Json.parse(json).validate[UploadStatus] mustBe JsError("Missing _type field")
      }
    }

    "UploadStatus writes" must {

      statuses.foreach {
        status =>
          s"set _type as $status" when {
            s"status is $status" in {
              val expectedJson = s"""{"_type":"$status"}"""
              Json.toJson(status).toString() mustBe expectedJson
            }
          }
      }

      "set _type as UploadedSuccessfully with name, downloadUrl and noOfRows in json" when {
        "status is UploadedSuccessfully" in {
          val expectedName = "fileName"
          val expectedUrl  = "downloadUrl"
          val expectedJson =
            s"""{"name":"$expectedName","downloadUrl":"$expectedUrl","_type":"UploadedSuccessfully"}"""
          val uploadStatus: UploadStatus = UploadedSuccessfully(expectedName, expectedUrl)
          Json.toJson(uploadStatus).toString() mustBe expectedJson
        }
      }

      "set _type as UploadRejected with error details in json" when {
        "status is UploadRejected" in {
          val errorDetails: ErrorDetails = ErrorDetails("REJECTED", "message")
          val expectedJson =
            s"""{"details":{"failureReason":"REJECTED","message":"message"},"_type":"UploadRejected"}"""
          val uploadStatus: UploadStatus = UploadRejected(errorDetails)
          Json.toJson(uploadStatus).toString() mustBe expectedJson
        }
      }
    }
  }
}
