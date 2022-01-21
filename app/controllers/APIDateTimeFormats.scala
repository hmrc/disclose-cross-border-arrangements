/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import models.subscription.cache.{CreateSubscriptionForDACRequest, SubscriptionForDACRequest}
import models.{SubmissionDetails, SubmissionHistory}
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object APIDateTimeFormats {

  lazy val localDateTimeWrites: Writes[LocalDateTime] =
    Writes.apply[LocalDateTime] {
      date => JsString(date.format(DateTimeFormatter.ISO_DATE_TIME))
    }

  implicit val writes: OWrites[SubmissionDetails] = (
    (JsPath \ "enrolmentID").write[String] and
      (JsPath \ "submissionTime").write[LocalDateTime](localDateTimeWrites) and
      (JsPath \ "fileName").write[String] and
      (JsPath \ "arrangementID").writeNullable[String] and
      (JsPath \ "disclosureID").writeNullable[String] and
      (JsPath \ "importInstruction").write[String] and
      (JsPath \ "initialDisclosureMA").write[Boolean] and
      (JsPath \ "messageRefId").write[String]
  )(unlift(SubmissionDetails.unapply))

  implicit val cacheWrites: OWrites[CreateSubscriptionForDACRequest] = (
    (__ \ "createSubscriptionForDACRequest").write[SubscriptionForDACRequest] and
      (__ \ "subscriptionID").write[String] and
      (__ \ "lastUpdated").write[LocalDateTime](localDateTimeWrites)
  )(
    r => (r.createSubscriptionForDACRequest, r.subscriptionID, r.lastUpdated)
  )

  implicit val format: OFormat[SubmissionHistory] = Json.format[SubmissionHistory]
}
