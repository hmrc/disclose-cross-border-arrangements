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
import models.subscription.{DisplaySubscriptionForDACRequest, UpdateSubscriptionForDACRequest}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConnector @Inject() (val config: AppConfig, val http: HttpClient) {
  private val logger: Logger = Logger(this.getClass)

  def displaySubscriptionForDAC(
    subscriptionForDACRequest: DisplaySubscriptionForDACRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val displaySubscriptionUrl = s"${config.registrationUrl}/dac6/dct04/v1"
    //x-conversation-id must match conversationID in RequestCommon otherwise EIS will throw a 400 Bad Request
    val conversationID = subscriptionForDACRequest.displaySubscriptionForDACRequest.requestCommon.conversationID.getOrElse("")

    val extraHeaders = Seq()
      .withBearerToken(s"${config.bearerToken}")
      .withXForwardedHost()
      .withDate()
      .withXCorrelationId()
      .withXConversationId(Some(conversationID))
      .withContentType()
      .withAccept()
      .withEnvironment(Some(config.eisEnvironment))
    logger.info(s"ExtraHeaders size = ${extraHeaders.size}")
    logger.debug(s"ExtraHeaders = $extraHeaders")

    http.POST[DisplaySubscriptionForDACRequest, HttpResponse](displaySubscriptionUrl, subscriptionForDACRequest, extraHeaders)(
      wts = DisplaySubscriptionForDACRequest.format,
      rds = httpReads,
      hc = hc,
      ec = ec
    )
  }

  def updateSubscriptionForDAC(
    updateSubscriptionForDACRequest: UpdateSubscriptionForDACRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val displaySubscriptionUrl = s"${config.registrationUrl}/dac6/dct05/v1"

    val extraHeaders = Seq()
      .withBearerToken(s"${config.bearerToken}")
      .withXForwardedHost()
      .withDate()
      .withXCorrelationId()
      .withXConversationId()
      .withContentType()
      .withAccept()
      .withEnvironment(Some(config.eisEnvironment))
    logger.info(s"ExtraHeaders size = ${extraHeaders.size}")
    logger.debug(s"ExtraHeaders = $extraHeaders")

    http.POST[UpdateSubscriptionForDACRequest, HttpResponse](displaySubscriptionUrl, updateSubscriptionForDACRequest, extraHeaders)(
      wts = UpdateSubscriptionForDACRequest.format,
      rds = httpReads,
      hc = hc,
      ec = ec
    )
  }

}
