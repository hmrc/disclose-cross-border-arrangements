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

import helpers.DateHelper
import javax.inject.Inject
import models.{ImportInstruction, SubmissionDetails, SubmissionHistory, SubmissionMetaData}
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.SubmissionDetailsRepository
import services.{AuditService, ContactService, SubmissionService, TransformService, XMLValidationService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      submissionService: SubmissionService,
                                      transformService: TransformService,
                                      contactService: ContactService,
                                      validationService: XMLValidationService,
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

        val conversationID = "" //TODO: Generate UUID

        val submissionMetaData = SubmissionMetaData.build(submissionTime, conversationID, fileName)

        for {
          ids <- submissionService.generateIDsForInstruction(importInstruction)

          //transform the file and add ids
          transformedFile = transformService.transformFileForIDs(submissionFile, ids)

          //get subscription data from SOMEWHERE (could be cache could be from HOD)
          subscriptionData <- contactService.getLatestContacts()

          //wrap data around the file to create submission payload
          submission: NodeSeq = transformService.addSubscriptionDetailsToSubmission(submissionFile, subscriptionData, submissionMetaData)

          //validate the payload
          (_, errors) = validationService.validateXml(submission.mkString)
        } yield {

          if(errors.nonEmpty){
            //TODO: Do something with the errors - probably needs to go into auditing
            //then throw an exception or return an internal server error
          }

          //submit to the backend
          //TODO

          auditService.submissionAudit(submissionFile, transformedFile)

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
          logger.error("Error generating and submitting declaration", ex)
          InternalServerError
      }
  }

  def getHistory(enrolmentId: String): Action[AnyContent] = Action.async {
    implicit request =>

      submissionDetailsRepository.retrieveSubmissionHistory(enrolmentId).map(
        history =>
          Ok(Json.toJson(SubmissionHistory(history)))
      ).recover {
        case ex:Exception =>
          logger.error("Error retrieving submission history", ex)
          InternalServerError
      }
  }

}
