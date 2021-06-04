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
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime}
import scala.xml.NodeSeq

case class SubmissionMetaData(submissionTime: String,
                              conversationID: String,
                              fileName: Option[String])

object SubmissionMetaData {
  val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

  def build(submissionTime: LocalDateTime,
            conversationID: String,
            fileName: String): SubmissionMetaData =
    SubmissionMetaData(
      dateTimeFormat.format(submissionTime.toInstant(OffsetDateTime.now().getOffset)),
      conversationID,
      Option(fileName)
    )
}

case class SubmissionDetails(enrolmentID: String,
                             submissionTime: LocalDateTime,
                             fileName: String,
                             arrangementID: Option[String],
                             disclosureID: Option[String],
                             importInstruction: String,
                             initialDisclosureMA: Boolean,
                             messageRefId: String)

object SubmissionDetails {

  import MongoJavatimeFormats.Implicits._

  def build(xml: NodeSeq,
            ids: GeneratedIDs,
            fileName: String,
            enrolmentID: String,
            importInstruction: ImportInstruction,
            disclosureID: String,
            submissionTime: LocalDateTime,
            initialDisclosureMA: Boolean,
            messageRefId: String): SubmissionDetails = {

    val arrID = Option {
      ids.arrangementID.map(_.value)
        .getOrElse((xml \\ "ArrangementID").text)
    }

    val discID = Option {
      ids.disclosureID.map(_.value)
        .getOrElse(disclosureID)
    }

    SubmissionDetails(
      enrolmentID = enrolmentID,
      submissionTime = submissionTime,
      fileName = fileName,
      arrangementID = arrID,
      disclosureID = discID,
      importInstruction = importInstruction.toString,
      initialDisclosureMA = initialDisclosureMA,
      messageRefId = messageRefId
    )
  }

  implicit val format: OFormat[SubmissionDetails] = Json.format[SubmissionDetails]
}

case class SubmissionHistory(details: Seq[SubmissionDetails])

object SubmissionHistory {

  implicit val format: OFormat[SubmissionHistory] = Json.format[SubmissionHistory]

  implicit val writes = Json.writes[SubmissionHistory]
}
