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

import helpers.DateHelper
import javax.inject.Inject
import models.{ImportInstruction, SubmissionDetails}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import repositories.SubmissionDetailsRepository
import services.{AuditService, SubmissionService, TransformService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      submissionService: SubmissionService,
                                      transformService: TransformService,
                                      dateHelper: DateHelper,
                                      submissionDetailsRepository: SubmissionDetailsRepository,
                                      auditService: AuditService
                                    )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  private val logger = LoggerFactory.getLogger(getClass)

  def storeSubmission: Action[NodeSeq] = Action.async(parse.xml) {
    implicit request =>
      {
        //receive xml and find import instructions
        val xml = request.body
        val fileName = (xml \ "fileName").text
        val enrolmentID = (xml \ "enrolmentID").text
        val importInstruction = ImportInstruction((xml \\ "DisclosureImportInstruction").text)
        val disclosureID = (xml \\ "DisclosureID").text
        val submissionFile: NodeSeq = (xml \ "file")
        val submissionTime = dateHelper.now
        val initialDisclosureMA = (xml \\ "InitialDisclosureMA").text.toBoolean

        for {
          ids <- submissionService.generateIDsForInstruction(importInstruction)

          //transform the file and store it
          transformedFile = transformService.transformFileForIDs(submissionFile, ids)
          submissionByteStream = new ByteArrayInputStream(transformedFile.mkString.getBytes)

          //filename altered to be as unique as possible
          //TODO: Removed gridFS submission - needs replacing with HOD submission
         /* _ <- storageService.writeFileToGridFS(
            FileName(fileName, disclosureID, ids, submissionTime).toString,
            Enumerator.fromStream(submissionByteStream)
          )*/

         _ =  auditService.submissionAudit(submissionFile, transformedFile)

        } yield {

          val submissionDetails = SubmissionDetails.build(
            xml = xml,
            ids = ids,
            fileName = fileName,
            enrolmentID = enrolmentID,
            importInstruction = importInstruction,
            disclosureID = disclosureID,
            submissionTime = submissionTime,
            initialDisclosureMA = initialDisclosureMA)

          submissionDetailsRepository.storeSubmissionDetails(submissionDetails)

          Ok(Json.toJson(ids))
        }
      } recover {
        case ex:Exception =>
          logger.error("Error storing to GridFS", ex)
          InternalServerError
      }
  }

}
