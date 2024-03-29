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

import com.google.inject.Inject
import controllers.auth.AuthAction
import models.upscan.{UploadId, UpscanIdentifiers}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.UploadSessionRepository
import services.UploadProgressTracker
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class UploadFormController @Inject() (
  authenticate: AuthAction,
  uploadProgressTracker: UploadProgressTracker,
  repository: UploadSessionRepository,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def requestUpload: Action[JsValue] = authenticate(parse.json).async {
    implicit request =>
      val upscanIdentifiers = request.body.validate[UpscanIdentifiers]
      upscanIdentifiers.fold(
        invalid = _ => Future.successful(BadRequest("")),
        valid = identifiers =>
          uploadProgressTracker
            .requestUpload(identifiers.uploadId, identifiers.fileReference)
            .map(
              _ => Ok
            )
      )
  }

  def getDetails(uploadId: String): Action[AnyContent] = authenticate.async {
    repository.findByUploadId(UploadId(uploadId)).map {
      case Some(value) => Ok(Json.toJson(value))
      case None        => NotFound
    }
  }

  def getStatus(uploadId: String): Action[AnyContent] = authenticate.async {
    uploadProgressTracker.getUploadResult(UploadId(uploadId)).map {
      case Some(value) => Ok(Json.toJson(value))
      case None        => NotFound
    }
  }
}
