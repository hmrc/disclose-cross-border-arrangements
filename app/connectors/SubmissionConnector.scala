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

package connectors

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import config.AppConfig
import javax.inject.Inject
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class SubmissionConnector @Inject()(
                                     val config: AppConfig,
                                     http: HttpClient
                                   )(implicit ex: ExecutionContext) {

  val submissionUrl = s"${config.submissionUrl}/dac6/dct06/v1"

  def submitDisclosure(submission: NodeSeq)(implicit hc:HeaderCarrier): Future[HttpResponse] = {
    val newHeaders: HeaderCarrier = hc
      .copy(authorization = Some(Authorization(s"Bearer ${config.bearerToken}")))
      .withExtraHeaders(addHeaders(): _*)

    http.POSTString(submissionUrl, submission.mkString, newHeaders.headers)
  }

  private def addHeaders()(implicit headerCarrier: HeaderCarrier): Seq[(String,String)] = {

    //HTTP-date format defined by RFC 7231 e.g. Fri, 01 Aug 2020 15:51:38 GMT+1
    val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O")

    Seq(
      "x-forwarded-host" -> "mdtp",
      "date" -> ZonedDateTime.now().format(formatter),
      "x-correlation-id" -> {
        headerCarrier.sessionId
          .map(_.value)
          .getOrElse(UUID.randomUUID().toString)
      },
      "x-conversation-id" -> {
        headerCarrier.requestId
          .map(_.value)
          .getOrElse(UUID.randomUUID().toString)
      },
      "content-type"    -> "application/xml",
      "accept"          -> "application/xml",
      "Environment"      -> config.eisEnvironment
    )
  }

}
