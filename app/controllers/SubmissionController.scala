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
import models.ImportInstruction
import play.api.Logger
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{GridFSStorageService, SubmissionService, TransformService}
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      submissionService: SubmissionService,
                                      transformService: TransformService,
                                      storageService: GridFSStorageService
                                    )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  def storeSubmission: Action[NodeSeq] = Action.async(parse.xml) {
    implicit request =>
     {
      //receive xml and find import instructions
      val xml = request.body
      val fileName = (xml \ "fileName").text
      val importInstruction = ImportInstruction((xml \\ "DisclosureImportInstruction").text)
      val submissionFile: NodeSeq = (xml \ "file")

      for {
        ids <- submissionService.generateIDsForInstruction(importInstruction)
        //transform the file and store it
        transformedFile = transformService.transformFileForIDs(submissionFile, ids)
        submissionByteStream = new ByteArrayInputStream(transformedFile.mkString.getBytes)
        _ <- storageService.writeFileToGridFS(fileName, Enumerator.fromStream(submissionByteStream))
        //TODO: Add audits for original and modified files
      } yield {
        Ok(Json.toJson(ids))
      }
     } recover {
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
