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

package models.subscription

import play.api.libs.json.{Json, OFormat, Reads, Writes, __}
import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class RequestCommonForUpdate(regime: String,
                                  receiptDate: String,
                                  acknowledgementReference: String,
                                  originatingSystem: String,
                                  requestParameters: Option[Seq[RequestParameter]])

object RequestCommonForUpdate {
  implicit val format: OFormat[RequestCommonForUpdate] = Json.format[RequestCommonForUpdate]
}

case class RequestDetailForUpdate(IDType: String,
                                  IDNumber: String,
                                  tradingName: Option[String],
                                  isGBUser: Boolean,
                                  primaryContact: PrimaryContact,
                                  secondaryContact: Option[SecondaryContact])
object RequestDetailForUpdate {
  implicit val reads: Reads[RequestDetailForUpdate] = (
    (__ \ "IDType").read[String] and
      (__ \ "IDNumber").read[String] and
      (__ \ "tradingName").readNullable[String] and
      (__ \ "isGBUser").read[Boolean] and
      (__ \ "primaryContact").read[Seq[PrimaryContact]] and
      (__ \ "secondaryContact").readNullable[Seq[SecondaryContact]]
    )((idt, idr, tn, gb, pc, sc) => RequestDetailForUpdate(idt, idr, tn, gb, pc.head, sc.map(_.head)))

  implicit lazy val writes: Writes[RequestDetailForUpdate] = (
    (__ \ "IDType").write[String] and
      (__ \ "IDNumber").write[String] and
      (__ \ "tradingName").writeNullable[String] and
      (__ \ "isGBUser").write[Boolean] and
      (__ \ "primaryContact").write[Seq[PrimaryContact]] and
      (__ \ "secondaryContact").writeNullable[Seq[SecondaryContact]]
    )(r => (r.IDType, r.IDNumber, r.tradingName, r.isGBUser, Seq(r.primaryContact), r.secondaryContact.map(Seq(_))))

}

case class UpdateSubscriptionDetails(requestCommon: RequestCommonForUpdate,
                                     requestDetail: RequestDetailForUpdate)
object UpdateSubscriptionDetails {
  implicit val format: OFormat[UpdateSubscriptionDetails] = Json.format[UpdateSubscriptionDetails]
}

case class UpdateSubscriptionForDACRequest(updateSubscriptionForDACRequest: UpdateSubscriptionDetails)
object UpdateSubscriptionForDACRequest {
  implicit val format: OFormat[UpdateSubscriptionForDACRequest] = Json.format[UpdateSubscriptionForDACRequest]
}
