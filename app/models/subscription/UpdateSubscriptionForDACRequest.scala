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

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import play.api.libs.json.{Json, OFormat}

import scala.util.Random


case class RequestCommonForUpdate(regime: String,
                                  receiptDate: String,
                                  acknowledgementReference: String,
                                  originatingSystem: String,
                                  requestParameters: Option[Seq[RequestParameter]])

object RequestCommonForUpdate {
  implicit val format: OFormat[RequestCommonForUpdate] = Json.format[RequestCommonForUpdate]

  def createRequestCommon: RequestCommonForUpdate = {
    //Format: ISO 8601 YYYY-MM-DDTHH:mm:ssZ e.g. 2020-09-23T16:12:11Z
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

    val r = new Random()
    val idSize: Int = 1 + r.nextInt(33) //Generate a size between 1 and 32
    val generateAcknowledgementReference: String = r.alphanumeric.take(idSize).mkString

    RequestCommonForUpdate(
      regime = "DAC",
      receiptDate = ZonedDateTime.now().format(formatter),
      acknowledgementReference = generateAcknowledgementReference,
      originatingSystem = "MDTP",
      requestParameters = None
    )
  }
}

case class RequestDetailForUpdate(IDType: String,
                                  IDNumber: String,
                                  tradingName: Option[String],
                                  isGBUser: Boolean,
                                  primaryContact: PrimaryContact,
                                  secondaryContact: Option[SecondaryContact])
object RequestDetailForUpdate {
  implicit val format: OFormat[RequestDetailForUpdate] = Json.format[RequestDetailForUpdate]
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
