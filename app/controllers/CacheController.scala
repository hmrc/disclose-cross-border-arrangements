/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.auth.{AuthAction, IdentifierAuthAction}
import models.ErrorDetails
import models.subscription.DisplaySubscriptionForDACRequest
import models.subscription.cache.CreateSubscriptionForDACRequest
import play.api.Logger
import play.api.libs.json.{JsResult, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import services.SubscriptionCacheService
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class CacheController @Inject() (
  authenticate: IdentifierAuthAction,
  authAction: AuthAction,
  subscriptionCacheService: SubscriptionCacheService,
  subscriptionConnector: SubscriptionConnector,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  import APIDateTimeFormats._

  private val logger: Logger = Logger(this.getClass)

  def storeSubscriptionDetails: Action[JsValue] = authAction.async(parse.json) {
    implicit request =>
      val subscriptionRequest = request.body.validate[CreateSubscriptionForDACRequest]

      subscriptionRequest.fold(
        invalid = errors => Future.successful(BadRequest("")),
        valid = createSubscription =>
          subscriptionCacheService.storeSubscriptionDetails(createSubscription.subscriptionID, createSubscription).map {
            _ => Ok
          }
      )
  }

  def updateSubscriptionDetails: Action[JsValue] = authenticate(parse.json).async {
    implicit request =>
      val subscriptionRequest = request.request.body.validate[CreateSubscriptionForDACRequest]

      subscriptionRequest.fold(
        invalid = errors => Future.successful(BadRequest("")),
        valid = createSubscription =>
          subscriptionCacheService.storeSubscriptionDetails(createSubscription.subscriptionID, createSubscription).map {
            _ => Ok
          }
      )
  }

  def retrieveSubscription: Action[JsValue] = authenticate(parse.json).async {
    implicit request =>
      val displaySubscriptionResult: JsResult[DisplaySubscriptionForDACRequest] =
        request.body.validate[DisplaySubscriptionForDACRequest]

      displaySubscriptionResult.fold(
        invalid = _ => Future.successful(BadRequest("")),
        valid = subResult =>
          subscriptionCacheService.retrieveSubscriptionDetails(subResult.displaySubscriptionForDACRequest.requestDetail.IDNumber).flatMap {
            case Some(result) => Future.successful(Ok(Json.toJson(result)))
            case None =>
              for {
                httpResponse <- subscriptionConnector.displaySubscriptionForDAC(subResult)
              } yield convertToResult(httpResponse)
          }
      )
  }

  private def convertToResult(httpResponse: HttpResponse): Result = {
    httpResponse.status match {
      case OK        => Ok(httpResponse.body)
      case NOT_FOUND => NotFound(httpResponse.body)

      case BAD_REQUEST =>
        logDownStreamError(httpResponse.body)
        BadRequest(httpResponse.body)

      case FORBIDDEN =>
        logDownStreamError(httpResponse.body)
        Forbidden(httpResponse.body)

      case METHOD_NOT_ALLOWED =>
        logDownStreamError(httpResponse.body)
        MethodNotAllowed(httpResponse.body)

      case CONFLICT =>
        logDownStreamError(httpResponse.body)
        Conflict(httpResponse.body)

      case INTERNAL_SERVER_ERROR =>
        logDownStreamError(httpResponse.body)
        InternalServerError(httpResponse.body)

      case _ =>
        logDownStreamError(httpResponse.body)
        ServiceUnavailable(httpResponse.body)
    }
  }

  private def logDownStreamError(body: String): Unit = {
    val error = Try(Json.parse(body).validate[ErrorDetails])
    error match {
      case Success(JsSuccess(value, _)) =>
        logger.error(s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}")
      case _ => logger.error("Error with submission but return is not a valid json")
    }
  }
}
