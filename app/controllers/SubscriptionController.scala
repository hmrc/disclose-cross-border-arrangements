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

package controllers

import connectors.SubscriptionConnector
import javax.inject.Inject
import models.DisplaySubscriptionForDACRequest
import play.api.libs.json.{JsResult, JsValue}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionController @Inject()(subscriptionConnector: SubscriptionConnector,
                                       cc: ControllerComponents
                                      )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def displaySubscriptionDetails: Action[JsValue] = Action(parse.json).async {
    implicit request =>

      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

      val displaySubscriptionResult: JsResult[DisplaySubscriptionForDACRequest] =
        request.body.validate[DisplaySubscriptionForDACRequest]

      displaySubscriptionResult.fold(
        invalid = _ => Future.successful(BadRequest("")),
        valid = request =>
          for {
            httpResponse <- subscriptionConnector.displaySubscriptionForDAC(request)
          } yield {
            convertToResult(httpResponse)
          }
      )
  }

  private def convertToResult(httpResponse: HttpResponse): Result = {
    httpResponse.status match {
      case OK => Ok(httpResponse.body)
      case BAD_REQUEST => BadRequest(httpResponse.body)
      case FORBIDDEN => Forbidden(httpResponse.body)
      case NOT_FOUND => NotFound(httpResponse.body)
      case METHOD_NOT_ALLOWED => MethodNotAllowed(httpResponse.body)
      case _ => InternalServerError(httpResponse.body)
    }
  }

}
