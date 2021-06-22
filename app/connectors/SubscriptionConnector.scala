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

package connectors

import config.AppConfig

import javax.inject.Inject
import models.subscription.{DisplaySubscriptionForDACRequest, UpdateSubscriptionForDACRequest}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HeaderNames, HttpClient, HttpResponse}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject()(val config: AppConfig, val http: HttpClient) {

  def displaySubscriptionForDAC(subscriptionForDACRequest: DisplaySubscriptionForDACRequest)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val displaySubscriptionUrl = s"${config.registrationUrl}/dac6/dct04/v1"
    //x-conversation-id must match conversationID in RequestCommon otherwise EIS will throw a 400 Bad Request
    val conversationID = subscriptionForDACRequest.displaySubscriptionForDACRequest.requestCommon.conversationID.getOrElse("")

    val extraHeader: Seq[(String, String)] = extraHeaders(hc, conversationID)

    http.POST[DisplaySubscriptionForDACRequest, HttpResponse](displaySubscriptionUrl, subscriptionForDACRequest, extraHeader)(wts =
      DisplaySubscriptionForDACRequest.format, rds = httpReads, hc = hc, ec = ec)
  }

  def updateSubscriptionForDAC(updateSubscriptionForDACRequest: UpdateSubscriptionForDACRequest)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val displaySubscriptionUrl = s"${config.registrationUrl}/dac6/dct05/v1"
    val conversationID = hc.sessionId.map(_.value).getOrElse(UUID.randomUUID().toString)

    val extraHeader: Seq[(String, String)] = extraHeaders(hc, conversationID)

    http.POST[UpdateSubscriptionForDACRequest, HttpResponse](displaySubscriptionUrl, updateSubscriptionForDACRequest, extraHeader)(wts =
      UpdateSubscriptionForDACRequest.format, rds = httpReads, hc = hc, ec = ec)
  }

  private def extraHeaders(hc: HeaderCarrier, conversationID: String)(implicit headerCarrier: HeaderCarrier): Seq[(String, String)] = {
    val newHeaders = hc
      .copy(authorization = Some(Authorization(s"Bearer ${config.bearerToken}")))

    newHeaders.headers(Seq(HeaderNames.authorisation)).++(addHeaders(conversationID))
  }

  private def addHeaders(conversationID: String)(implicit headerCarrier: HeaderCarrier): Seq[(String,String)] = {

    //HTTP-date format defined by RFC 7231 e.g. Fri, 01 Aug 2020 15:51:38 GMT+1
    val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O")

    Seq(
      "date" -> ZonedDateTime.now().format(formatter),
      "x-correlation-id" -> {
        headerCarrier.requestId
          .map(_.value)
          .getOrElse(UUID.randomUUID().toString)
      },
      "x-conversation-id" -> conversationID,
      "x-forwarded-host" -> "mdtp",
      "content-type"    -> "application/json",
      "accept"          -> "application/json",
      "Environment"      -> config.eisEnvironment
    )
  }

}
