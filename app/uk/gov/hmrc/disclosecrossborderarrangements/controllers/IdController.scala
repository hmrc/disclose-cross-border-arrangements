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

package uk.gov.hmrc.disclosecrossborderarrangements.controllers

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.disclosecrossborderarrangements.config.AppConfig
import uk.gov.hmrc.disclosecrossborderarrangements.models.ArrangementId
import uk.gov.hmrc.disclosecrossborderarrangements.services.IdService
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.global

class IdController @Inject()(idService: IdService, appConfig: AppConfig, cc: ControllerComponents)
                            (implicit executionContext: ExecutionContext)
extends BackendController(cc) {

  val arrangementIdRegEx = "[A-Z]{2}[A]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"

  def verifyArrangementId(arrangementId: String): Action[AnyContent] = Action.async { implicit request =>
      idService.verifyArrangementId(arrangementId) map {
        case Some(true) => NoContent
        case Some(false) => NotFound("Arrangement Id does not exist")
        case None => BadRequest("invalid format")
  }
}
}
