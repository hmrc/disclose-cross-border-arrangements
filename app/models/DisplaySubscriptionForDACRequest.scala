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

import play.api.libs.json.{Json, OFormat}

case class RequestParameter(paramName: String,
                            paramValue: String)
object RequestParameter {
  implicit val format: OFormat[RequestParameter] = Json.format[RequestParameter]
}

case class RequestCommon(regime: String,
                         receiptDate: String,
                         acknowledgementReference: String,
                         originatingSystem: String,
                         requestParameters: Option[Seq[RequestParameter]])

object RequestCommon {
  implicit val format: OFormat[RequestCommon] = Json.format[RequestCommon]
}

case class RequestDetail(IDType: String, IDNumber: String)
object RequestDetail {
  implicit val format: OFormat[RequestDetail] = Json.format[RequestDetail]
}

case class DisplaySubscriptionDetails(requestCommon: RequestCommon,
                                      requestDetail: RequestDetail)
object DisplaySubscriptionDetails {
  implicit val format: OFormat[DisplaySubscriptionDetails] = Json.format[DisplaySubscriptionDetails]
}

case class DisplaySubscriptionForDACRequest(displaySubscriptionForDACRequest: DisplaySubscriptionDetails)
object DisplaySubscriptionForDACRequest {
  implicit val format: OFormat[DisplaySubscriptionForDACRequest] = Json.format[DisplaySubscriptionForDACRequest]
}
