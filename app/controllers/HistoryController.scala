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

import javax.inject.Inject
import models.SubmissionHistory
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.SubmissionDetailsRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class HistoryController @Inject()(
                                   cc: ControllerComponents,
                                   submissionDetailsRepository: SubmissionDetailsRepository
                                 )(implicit ec: ExecutionContext) extends BackendController(cc) {

  def noOfPreviousSubmissions(enrolmentId: String): Action[AnyContent] = Action.async {
    implicit request =>
     submissionDetailsRepository
      .countNoOfPreviousSubmissions(enrolmentId)
      .map(no => Ok(s"$no"))
  }

  def submissionDetails(enrolmentId: String): Action[AnyContent] = Action.async {
    implicit request =>
      submissionDetailsRepository
        .retrieveSubmissionHistory(enrolmentId)
        .map {
          details =>
            Ok(Json.toJson(SubmissionHistory(details)))
        }.recover {
          case e =>
            InternalServerError(s"Failed with the following error: $e")
        }
  }
}