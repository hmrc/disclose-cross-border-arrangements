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

import java.util.UUID

import connectors.SubmissionConnector
import helpers.DateHelper
import javax.inject.Inject
import models._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.SubmissionDetailsRepository
import services._
import uk.gov.hmrc.http.HeaderNames.xSessionId
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import scala.xml.NodeSeq

class SubmissionController @Inject()(
                                      cc: ControllerComponents,
                                      submissionService: SubmissionService,
                                      transformService: TransformService,
                                      contactService: ContactService,
                                      validationService: XMLValidationService,
                                      submissionConnector: SubmissionConnector,
                                      dateHelper: DateHelper,
                                      submissionDetailsRepository: SubmissionDetailsRepository,
                                      auditService: AuditService
                                    )(implicit ec: ExecutionContext)
  extends BackendController(cc) {

  private val logger = LoggerFactory.getLogger(getClass)

  def submitDisclosure: Action[NodeSeq] = Action.async(parse.xml) {
    implicit request =>
      {
        //receive xml and find import instructions
        val xml = request.body
        val fileName = (xml \ "fileName").text
        val enrolmentID = (xml \ "enrolmentID").text
        val importInstruction = ImportInstruction((xml \\ "DisclosureImportInstruction").text)
        val disclosureID = (xml \\ "DisclosureID").text
        val submissionFile: NodeSeq = (xml \ "file" \ "DAC6_Arrangement")
        val submissionTime = dateHelper.now
        val initialDisclosureMA = (xml \\ "InitialDisclosureMA").text.toBoolean

        val conversationID: String = hc.headers
          .find(_._1 == xSessionId).map(_._2)
          .getOrElse(UUID.randomUUID().toString)

        val submissionMetaData = SubmissionMetaData.build(submissionTime, conversationID, fileName)

        for {
          ids <- submissionService.generateIDsForInstruction(importInstruction)

          //transform the file and add ids
          transformedFile = transformService.transformFileForIDs(submissionFile, ids)

          //get subscription data from SOMEWHERE (could be cache could be from HOD)
          subscriptionData <- contactService.getLatestContacts(enrolmentID)

          //wrap data around the file to create submission payload
          submission: NodeSeq = transformService.addSubscriptionDetailsToSubmission(submissionFile, subscriptionData, submissionMetaData)

          //change namespaces
          disclosureSubmission: NodeSeq  = transformService.addNameSpaces(submission, Seq(
              NamespaceForNode("DAC6UKSubmissionInboundRequest", "eis"),
              NamespaceForNode("DAC6_Arrangement", "dac6")
            ))

          //validate the payload
          _ = validationService.validateXml(disclosureSubmission.mkString).left.map {
            errors =>
              auditService.auditValidationFailures(enrolmentID, errors)

              //then throw an exception or return an internal server error
              throw new Exception("There have been errors in validating the submission payload")
          }

          //submit to the backend
          response <- submissionConnector.submitDisclosure(disclosureSubmission)

        } yield {
          if(response.status == OK) {
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

            submissionDetailsRepository.storeSubmissionDetails(submissionDetails).map {
              succeeded =>
                if(succeeded) Ok(Json.toJson(ids))
                else {
                  logger.error("Unable to store submission detail to database")
                  throw new Exception("Unable to store submission detail to database")
                }
            }
          } else {
            //TODO: Can we release the generated ids for a downstream error?
            Future.successful(convertToResult(response))
          }
        }
      }.flatMap(identity) recover {
        case ex: Exception =>
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

  private def convertToResult(httpResponse: HttpResponse): Result = {
    httpResponse.status match {
      case OK => Ok(httpResponse.body)
      case NOT_FOUND => NotFound(httpResponse.body)
      case BAD_REQUEST => {
        val error = Try(Json.parse(httpResponse.body).validate[ErrorDetails])
        error match {
          case Success(JsSuccess(value, _)) =>
            logger.error(s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}")
          case _ => logger.error("Error with submission but return is not a valid json")
        }
        BadRequest(httpResponse.body)
      }
      case FORBIDDEN => {
        val error = Try(Json.parse(httpResponse.body).validate[ErrorDetails])
        error match {
          case Success(JsSuccess(value, _)) =>
            logger.error(s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}")
          case _ => logger.error("Error with submission but return is not a valid json")
        }
        Forbidden(httpResponse.body)
      }
      case METHOD_NOT_ALLOWED => {
        val error = Try(Json.parse(httpResponse.body).validate[ErrorDetails])
        error match {
          case Success(JsSuccess(value, _)) =>
            logger.error(s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}")
          case _ => logger.error("Error with submission but return is not a valid json")
        }
        MethodNotAllowed(httpResponse.body)
      }
      case CONFLICT => {
        val error = Try(Json.parse(httpResponse.body).validate[ErrorDetails])
        error match {
          case Success(JsSuccess(value, _)) =>
            logger.error(s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}")
          case _ => logger.error("Error with submission but return is not a valid json")
        }
        Conflict(httpResponse.body)
      }
      case INTERNAL_SERVER_ERROR => {
        val error = Try(Json.parse(httpResponse.body).validate[ErrorDetails])
        error match {
          case Success(JsSuccess(value, _)) =>
            logger.error(s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}")
          case _ => logger.error("Error with submission but return is not a valid json")
        }
        InternalServerError(httpResponse.body)
      }
      case _ => {
        val error = Try(Json.parse(httpResponse.body).validate[ErrorDetails])
        error match {
          case Success(JsSuccess(value, _)) =>
            logger.error(s"Error with submission: ${value.errorDetail.sourceFaultDetail.map(_.detail.mkString)}")
          case _ => logger.error("Error with submission but return is not a valid json")
        }
        ServiceUnavailable(httpResponse.body)
      }
    }
  }

}
