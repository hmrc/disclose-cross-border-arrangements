/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.auth.IdentifierAuthAction
import models.{ManualSubmissionValidationFailure, ManualSubmissionValidationSuccess}
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import services.ManualSubmissionValidationEngine
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class ManualSubmissionValidationController @Inject() (identify: IdentifierAuthAction,
                                                      cc: ControllerComponents,
                                                      validationEngine: ManualSubmissionValidationEngine
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def validateManualSubmission: Action[NodeSeq] = identify.async(parse.xml) {
    implicit request =>
      validationEngine.validateManualSubmission(request.body, request.enrolmentID) map {
        case Some(ManualSubmissionValidationSuccess(messageRefId)) => Ok(Json.toJson(ManualSubmissionValidationSuccess(messageRefId)))
        case Some(ManualSubmissionValidationFailure(Seq(errors)))  => Ok(Json.toJson(ManualSubmissionValidationFailure(Seq(errors))))
        case None                                                  => BadRequest("Invalid_XML")
      }
  }
}
