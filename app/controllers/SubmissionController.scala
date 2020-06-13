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

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import javax.inject.Inject
import play.api.Logger
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.GridFSStorageService
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      storageService: GridFSStorageService
                                    )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def storeSubmission: Action[NodeSeq] = Action.async(parse.xml) {
    implicit request =>
      //receive xml and find import instructions
      //generate arrangementID and disclosure ID (based on instruction type)
      //transform the file and store it
      //audit original page and transformed page


      val xml = request.body
      val fileName = (xml \ "fileName").text
      val submissionFile = (xml \ "file").mkString

      val submissionByteStream = new ByteArrayInputStream(submissionFile.getBytes)


      storageService
        .writeFileToGridFS(fileName, Enumerator.fromStream(submissionByteStream))
        .map(_ => Ok).recover {
        case ex:Exception =>
          Logger.error("Error storing to GridFS", ex)
          InternalServerError
      }
  }

  def readSubmissionFromStore(fileName: String): Action[AnyContent] = Action.async {
    implicit request =>

      storageService
        .readFileFromGridFS(fileName).map {
        case Some(bytes) => Ok(new String(bytes, StandardCharsets.UTF_8))
        case None => NotFound
      }.recover {
        case ex:Exception =>
          Logger.error("Error reading from GridFS", ex)
          InternalServerError
      }
  }

}
