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

import helpers.DateHelper
import javax.inject.Inject
import models.{FileName, GeneratedIDs, ImportInstruction, SubmissionDetails}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.SubmissionDetailsRepository
import services.{GridFSStorageService, SubmissionService, TransformService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      submissionService: SubmissionService,
                                      transformService: TransformService,
                                      storageService: GridFSStorageService,
                                      dateHelper: DateHelper,
                                      submissionDetailsRepository: SubmissionDetailsRepository
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
          _ <- storageService.writeFileToGridFS(
            FileName(fileName, disclosureID, ids, submissionTime).toString,
            Enumerator.fromStream(submissionByteStream)
          )
          //TODO: Add audits for original and modified files
        } yield {

          val submissionDetails = buildSubmissionDetails(
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

  def readSubmissionFromStore(fileName: String): Action[AnyContent] = Action.async {
    implicit request =>

      storageService
        .readFileFromGridFS(fileName).map {
        case Some(bytes) => Ok(new String(bytes, StandardCharsets.UTF_8))
        case None => NotFound
      }.recover {
        case ex:Exception =>
          logger.error("Error reading from GridFS", ex)
          InternalServerError
      }
  }

  private def buildSubmissionDetails(xml: NodeSeq,
                                     ids: GeneratedIDs,
                                     fileName: String,
                                     enrolmentID: String,
                                     importInstruction: ImportInstruction,
                                     disclosureID: String,
                                     submissionTime: DateTime,
                                     initialDisclosureMA: Boolean): SubmissionDetails = {

    val arrID = Option {
      ids.arrangementID.map(_.value)
        .getOrElse((xml \\ "ArrangementID").text)
    }

    val discID = Option {
      ids.disclosureID.map(_.value)
        .getOrElse(disclosureID)
    }

    SubmissionDetails(
      enrolmentID = enrolmentID,
      submissionTime = submissionTime,
      fileName = fileName,
      arrangementID = arrID,
      disclosureID = discID,
      importInstruction = importInstruction.toString,
      initialDisclosureMA = initialDisclosureMA
    )
  }

}
