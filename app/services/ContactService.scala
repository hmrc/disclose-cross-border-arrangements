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

package services

import connectors.SubscriptionConnector
import javax.inject.Inject
import models._
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ContactService @Inject()(subscriptionConnector: SubscriptionConnector) {

  //TODO: find where to get latest contact data from
  def getLatestContacts(enrolmentID: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[SubscriptionDetails] = {


    val subscriptionForDACRequest: DisplaySubscriptionForDACRequest =
      DisplaySubscriptionForDACRequest(DisplaySubscriptionDetails(
        RequestCommon.createRequestCommon,
        RequestDetail(
          IDType = "DAC",
          IDNumber = enrolmentID)
      ))

    subscriptionConnector.displaySubscriptionForDAC(subscriptionForDACRequest).map {
      response =>
        response.status match {
          case OK => response.json.validate[DisplaySubscriptionForDACResponse] match {
            case JsSuccess(response, _) => Some(response)
            case JsError(_) => None
          }
          case _ => None
        }
    } map {
      retrievedSubscription =>
        retrievedSubscription.map {
          sub =>
            val details = sub.displaySubscriptionForDACResponse.responseDetail

          SubscriptionDetails(
            subscriptionID = details.subscriptionID,
            tradingName = details.tradingName,
            isGBUser = details.isGBUser,
            primaryContact = details.primaryContact.contactInformation.head,
            secondaryContact = details.secondaryContact.map(_.contactInformation.head)
          )

        } getOrElse(throw new Exception("Failed to retrieve and convert subscription"))

    }
  }

}
