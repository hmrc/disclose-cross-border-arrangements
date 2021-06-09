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

import play.api.libs.json.{Json, OFormat}

sealed trait ManualSubmissionValidationResult

case class ManualSubmissionValidationSuccess(messageRefId: String) extends ManualSubmissionValidationResult

object ManualSubmissionValidationSuccess {
  implicit val format: OFormat[ManualSubmissionValidationSuccess] = Json.format[ManualSubmissionValidationSuccess]
}

case class ManualSubmissionValidationFailure(errors: Seq[String]) extends ManualSubmissionValidationResult

object ManualSubmissionValidationFailure {
  implicit val format: OFormat[ManualSubmissionValidationFailure] = Json.format[ManualSubmissionValidationFailure]
}

//TODO - add models for UploadSubmissionResult - DAC6-858

sealed trait UploadSubmissionValidationResult

case class UploadSubmissionValidationSuccess(dac6MetaData: Dac6MetaData) extends UploadSubmissionValidationResult

object UploadSubmissionValidationSuccess {
  implicit val format: OFormat[UploadSubmissionValidationSuccess] = Json.format[UploadSubmissionValidationSuccess]
}

case class UploadSubmissionValidationFailure(errors: Seq[String]) extends UploadSubmissionValidationResult

object UploadSubmissionValidationFailure {
  implicit val format: OFormat[UploadSubmissionValidationFailure] = Json.format[UploadSubmissionValidationFailure]
}
