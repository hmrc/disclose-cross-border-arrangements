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

package controllers.auth

import com.google.inject.ImplementedBy
import controllers.Assets.Status
import models.UserRequest
import org.slf4j.LoggerFactory
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.{ActionBuilder, ActionFunction, AnyContent, BodyParsers, Request, Result}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IdentifierAuthActionImpl @Inject()(
                                          override val authConnector: AuthConnector,
                                          val parser: BodyParsers.Default
                              )(implicit val executionContext: ExecutionContext)
  extends IdentifierAuthAction with AuthorisedFunctions {
  private val logger = LoggerFactory.getLogger(getClass)

  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)

    authorised().retrieve(Retrievals.internalId) {
      case Some(internalID) => block(UserRequest(internalID, request))
      case None => Future.successful(Status(UNAUTHORIZED))
    } recover {
      case _: NoActiveSession =>
        Status(UNAUTHORIZED)
    }
  }

}

@ImplementedBy(classOf[IdentifierAuthActionImpl])
trait IdentifierAuthAction extends ActionBuilder[UserRequest, AnyContent] with ActionFunction[Request, UserRequest]
