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

import helpers.{ErrorMessageHelper, XmlErrorMessageHelper}
import models._
import org.slf4j.LoggerFactory
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.Elem

class UploadSubmissionValidationEngine @Inject() (xmlValidationService: XMLValidationService,
                                                  businessRuleValidationService: BusinessRuleValidationService,
                                                  metaDataValidationService: MetaDataValidationService,
                                                  xmlErrorMessageHelper: XmlErrorMessageHelper,
                                                  errorMessageHelper: ErrorMessageHelper,
                                                  auditService: AuditService
) {

  private val logger   = LoggerFactory.getLogger(getClass)
  private val noErrors = Seq()

  def validateUploadSubmission(upScanUrl: Option[String], enrolmentId: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[UploadSubmissionValidationResult]] = {

    val xmlUrl = upScanUrl.fold(throw new Exception("Unable to retrieve XML from Upscan URL"))(
      xmlLocation => xmlLocation
    )

    try {
      val xmlAndXmlValidationStatus: (Elem, Seq[GenericError]) = performXmlValidation(xmlUrl)
      val metaData: Option[Dac6MetaData]                       = businessRuleValidationService.extractDac6MetaData()(xmlAndXmlValidationStatus._1)

      for {
        metaDataResult      <- metaDataValidationService.verifyMetaDataForUploadSubmission(metaData, enrolmentId, xmlAndXmlValidationStatus._1)
        businessRulesResult <- performBusinessRulesValidation(xmlAndXmlValidationStatus._1)
      } yield {

        combineUploadResults(xmlAndXmlValidationStatus._2, businessRulesResult, metaDataResult) match {
          case Seq() =>
            Some(UploadSubmissionValidationSuccess(metaDataResult.right.get))
          case errors: Seq[GenericError] =>
            auditService.auditUploadSubmissionFailure(enrolmentId, metaData, errors)
            Some(UploadSubmissionValidationFailure(errors))
          case _ =>
            None
        }
      }
    } catch {
      case e: Exception =>
        logger.warn(s"XML parsing failed. The XML parser has thrown the exception: $e")
        Future.successful(None)
    }
  }

  def performXmlValidation(xmlUrl: String): (Elem, Seq[GenericError]) = {

    val xmlErrors: (Elem, ListBuffer[SaxParseError]) = xmlValidationService.validateUploadXml(xmlUrl)
    if (xmlErrors._2.isEmpty) {
      (xmlErrors._1, noErrors)
    } else {
      val filteredErrors: Seq[GenericError] = xmlErrorMessageHelper.generateErrorMessages(xmlErrors._2)
      (xmlErrors._1, filteredErrors)
    }
  }

  def performBusinessRulesValidation(elem: Elem)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[GenericError]] = {

    businessRuleValidationService.validateFile()(hc, ec)(elem) match {
      case Some(value) =>
        value.map {
          seqValidation: Seq[Validation] =>
            errorMessageHelper.convertToGenericErrors(seqValidation, elem)
        }
      case None => Future.successful(noErrors)
    }
  }

  private def combineUploadResults(xmlResult: Seq[GenericError],
                                   businessRulesResult: Seq[GenericError],
                                   metaDataResult: Either[Seq[GenericError], Dac6MetaData]
  ): Seq[GenericError] = {

    val combinedErrors = (xmlResult ++ businessRulesResult ++ metaDataResult.left.getOrElse(Seq.empty)).sortBy(_.lineNumber)

    if (combinedErrors.isEmpty)
      noErrors
    else {
      combinedErrors
    }
  }
}
