/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import config.AppConfig
import models.{Dac6MetaData, GenericError, SaxParseError}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Disabled, Failure}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

class AuditService @Inject()(appConfig: AppConfig, auditConnector: AuditConnector)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)
  val noneProvided = "None Provided"

  // match auditing with diclose-frontend - DAC6-858

  def auditValidationFailures(subscriptionID: String, errors: Seq[SaxParseError])(implicit hc: HeaderCarrier): Unit = {
    val auditType = "Validation"
    val transactionName = "/disclose-cross-border-arrangements/validation"
    val path = "/disclose-cross-border-arrangements/validation"

    val auditJson = Json.obj(
      "subscriptionID" -> subscriptionID,
      "validationErrors" -> errors
        .map(error => s"${error.lineNumber}: ${error.errorMessage}")
        .mkString(",")
    )

    auditConnector.sendExtendedEvent(ExtendedDataEvent(
      auditSource = appConfig.appName,
      auditType = auditType,
      detail = auditJson,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails()
      ++ AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName, path)
    )) map { ar: AuditResult => ar match {
        case Failure(msg, ex) =>
        ex match {
        case Some(throwable) =>
        logger.warn(s"The attempt to issue audit event $auditType failed with message : $msg", throwable)
        case None =>
        logger.warn(s"The attempt to issue audit event $auditType failed with message : $msg")
      }
        ar
        case Disabled =>
        logger.warn(s"The attempt to issue audit event $auditType was unsuccessful, as auditing is currently disabled in config"); ar
        case _ => logger.debug(s"Audit event $auditType issued successsfully."); ar
      }}
  }

  def auditManualSubmissionParseFailure(enrolmentID: String, metaData: Option[Dac6MetaData], errors: ListBuffer[SaxParseError])(implicit hc: HeaderCarrier): Unit = {

    val auditType = "ManualSubmissionParseFailure"

    val auditMap: JsObject = Json.obj(
      "enrolmentID" -> enrolmentID,
      "arrangementID" -> metaData.fold(noneProvided)(data => data.arrangementID.getOrElse(noneProvided)),
      "disclosureID" ->metaData.fold(noneProvided)(data => data.disclosureID.getOrElse(noneProvided)),
      "messageRefID" -> metaData.fold(noneProvided)(data => data.messageRefId),
      "disclosureImportInstruction" -> metaData.fold("Unknown Import Instruction")(data => data.importInstruction),
      "initialDisclosureMA" -> metaData.fold("InitialDisclosureMA value not supplied")(data => data.initialDisclosureMA.toString),
      "errors" -> buildManualErrorPayload(errors))

    if(appConfig.validationAuditToggle) {
      auditConnector.sendExtendedEvent(ExtendedDataEvent(
        auditSource = appConfig.appName,
        auditType = auditType,
        detail = auditMap,
        tags = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails()
      )) map { ar: AuditResult =>
        ar match {
          case Failure(msg, ex) =>
            ex match {
              case Some(throwable) =>
                logger.warn(s"The attempt to issue audit event $auditType failed with message : $msg", throwable)
              case None =>
                logger.warn(s"The attempt to issue audit event $auditType failed with message : $msg")
            }
            ar
          case Disabled =>
            logger.warn(s"The attempt to issue audit event $auditType was unsuccessful, as auditing is currently disabled in config"); ar
          case _ => logger.debug(s"Audit event $auditType issued successfully."); ar
        }
      }
    } else {
      logger.warn(s"Validation has failed and auditing currently disabled for this event type")
    }
  }

  def auditUploadSubmissionParseFailure(enrolmentId: String, metaData: Option[Dac6MetaData], errors: Seq[GenericError])
                                       (implicit hc: HeaderCarrier): Unit = {

    val validationFailureType = "UploadSubmissionParseFailure"

    val auditMap: JsObject = Json.obj(
      "enrolmentID" -> enrolmentId,
      "arrangementID" -> metaData.fold(noneProvided)(data => data.arrangementID.getOrElse(noneProvided)),
      "disclosureID" ->metaData.fold(noneProvided)(data => data.disclosureID.getOrElse(noneProvided)),
      "messageRefID" -> metaData.fold(noneProvided)(data => data.messageRefId),
      "disclosureImportInstruction" -> metaData.fold("Unknown Import Instruction")(data => data.importInstruction),
      "initialDisclosureMA" -> metaData.fold("InitialDisclosureMA value not supplied")(data => data.initialDisclosureMA.toString),
      "errors" -> buildUploadErrorPayload(errors),
    )

    if(appConfig.validationAuditToggle) {
      auditConnector.sendExtendedEvent(ExtendedDataEvent(
        auditSource = appConfig.appName,
        auditType = validationFailureType,
        detail = auditMap,
        tags = AuditExtensions.auditHeaderCarrier(hc).toAuditDetails()
      )) map { ar: AuditResult =>
        ar match {
          case Failure(msg, ex) =>
            ex match {
              case Some(throwable) =>
                logger.warn(s"The attempt to issue audit event $validationFailureType failed with message : $msg", throwable)
              case None =>
                logger.warn(s"The attempt to issue audit event $validationFailureType failed with message : $msg")
            }
            ar
          case Disabled =>
            logger.warn(s"The attempt to issue audit event $validationFailureType was unsuccessful, as auditing is currently disabled in config"); ar
          case _ => logger.debug(s"Audit event $validationFailureType issued successfully."); ar
        }
      }
    } else {
      logger.warn(s"Validation has failed and auditing currently disabled for this event type")
    }
  }


  private def buildManualErrorPayload(errors: ListBuffer[SaxParseError]): String = {

    val formattedErrors = errors.map {error =>

      s"""|{
          |"lineNumber" : ${error.lineNumber},
          |"errorMessage" : ${error.errorMessage}
          |}""".stripMargin
    }.mkString(",")


    s"""|[
        |$formattedErrors
        |]""".stripMargin.mkString

  }

  private def buildUploadErrorPayload(errors: Seq[GenericError]): String = {

    val formattedErrors = errors.map {error =>
      s"""|{
          |"lineNumber" : ${error.lineNumber},
          |"errorMessage" : ${error.messageKey}
          |}""".stripMargin
    }.mkString(",")

    s"""|[
        |$formattedErrors
        |]""".stripMargin.mkString

  }
}
