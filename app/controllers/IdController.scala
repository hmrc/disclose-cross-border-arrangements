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

import controllers.auth.AuthAction
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IdController @Inject() (
  authenticate: AuthAction,
  idService: IdService,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext)
    extends BackendController(cc) {

  val arrangementIdRegEx = "[A-Z]{2}[A]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"

  def verifyArrangementId(arrangementId: String): Action[AnyContent] = authenticate.async {
    implicit request =>
      idService.verifyArrangementId(arrangementId) map {
        case Some(true)  => NoContent
        case Some(false) => NotFound("Arrangement Id does not exist")
        case None        => BadRequest("invalid format")
      }
  }

  def verifyDisclosureIDs(arrangementId: String, disclosureId: String, enrolmentId: String): Action[AnyContent] = authenticate.async {
    implicit request =>
      val arrangementIDNotFound = "Arrangement ID not found"
      val disclosureIDNotFound  = "Disclosure ID doesn't match enrolment ID"
      val idsDoNotMatch         = "Arrangement ID and Disclosure ID are not from the same submission"
      val idsNotFound           = "IDs not found"

      idService.verifyIDs(arrangementId, disclosureId, enrolmentId) map {
        case IdsCorrect                => NoContent
        case IdsInDifferentSubmissions => NotFound(idsDoNotMatch)
        case ArrangementIDNotFound     => NotFound(arrangementIDNotFound)
        case DisclosureIDNotFound      => NotFound(disclosureIDNotFound)
        case IdsNotFound               => BadRequest(idsNotFound)
      }
  }
}
