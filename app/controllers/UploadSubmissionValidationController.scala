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

package controllers

import controllers.auth.IdentifierAuthAction
import models.{UploadSubmissionValidationFailure, UploadSubmissionValidationSuccess}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.UploadSubmissionValidationEngine
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UploadSubmissionValidationController @Inject()(identify: IdentifierAuthAction,
                                                     cc: ControllerComponents,
                                                     validationEngine: UploadSubmissionValidationEngine,
                                                    )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def validateUploadSubmission: Action[AnyContent] =  identify.async {
    implicit request =>
      validationEngine.validateUploadSubmission(request.body.asText.get, request.enrolmentID) map {
        case Some(UploadSubmissionValidationSuccess(dac6metaData)) => Ok(Json.toJson(UploadSubmissionValidationSuccess(dac6metaData)))
        case Some(UploadSubmissionValidationFailure(Seq(errors))) => Ok(Json.toJson(UploadSubmissionValidationFailure(Seq(errors))))
        case None => BadRequest("Invalid_XML")
      }
  }
}
