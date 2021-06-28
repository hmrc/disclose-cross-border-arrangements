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
import scala.xml.{Elem, NodeSeq}

class UploadSubmissionValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                                 businessRuleValidationService: BusinessRuleValidationService,
                                                 metaDataValidationService: MetaDataValidationService,
                                                 xmlErrorMessageHelper: XmlErrorMessageHelper,
                                                 errorMessageHelper: ErrorMessageHelper,
                                                 auditService: AuditService) {

  private val logger = LoggerFactory.getLogger(getClass)
  private val noErrors = Seq.empty

  //TODO - Change output to submission Result - DAC6-858
  //TODO - Pass back metadata instead of MessageRefID
  def validateUploadSubmission(xml: NodeSeq, enrolmentId: String)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[Option[UploadSubmissionValidationResult]] = {

    val elem: Elem = xml.asInstanceOf[Elem]

    try {

      val xmlValidationResult: Seq[GenericError] = performXmlValidation(elem)
      val metaData: Option[Dac6MetaData] = businessRuleValidationService.extractDac6MetaData()(elem)

      for {
        metaDataResult <- metaDataValidationService.verifyMetaDataForUploadSubmission(metaData, enrolmentId, elem)
        businessRulesResult <- performBusinessRulesValidation(elem)
      } yield {

        combineUploadResults(xmlValidationResult, businessRulesResult, metaDataResult) match {
          case Seq() =>
            println("\n\n------------success------------\n\n")
            Some(UploadSubmissionValidationSuccess(metaDataResult.right.get))
          case Seq(errors) =>
            println("\n\n------------failure------------\n\n")
        //auditService.auditManualSubmissionParseFailure(enrolmentId, metaData, xmlAndXmlValidationStatus)
          Some(UploadSubmissionValidationFailure(Seq(errors)))

          case _ =>
            println("\n------------errors------------\n")
            None
        }
      }
    } catch {
      case e: Exception =>
        logger.warn(s"XML validation failed. The XML parser has thrown the exception: $e")
        Future.successful(None)
    }
  }

  def performXmlValidation(elem: Elem): Seq[GenericError] = {
    val xmlErrors: ListBuffer[SaxParseError] = xmlValidationService.validateManualSubmission(elem)
    if (xmlErrors.isEmpty) {
      noErrors
    } else {
      xmlErrorMessageHelper.generateErrorMessages(xmlErrors)
    }
  }

  def performBusinessRulesValidation(elem: Elem)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[GenericError]] = {

    businessRuleValidationService.validateFile()(hc, ec)(elem) match {
      case Some(value) => value.map {
        seqValidation: Seq[Validation] =>
          errorMessageHelper.convertToGenericErrors(seqValidation, elem)
      }
      case None => Future.successful(noErrors)
    }
  }

  private def combineUploadResults(xmlResult: Seq[GenericError], businessRulesResult: Seq[GenericError],
                             metaDataResult:  Either[Seq[GenericError], Dac6MetaData]):  Seq[GenericError] = {

    val combinedErrors =
      (xmlResult ++ businessRulesResult ++ metaDataResult.left.getOrElse(Seq.empty)).sortBy(_.lineNumber)


    println(s"\n\n-------combined errors----------: $combinedErrors\n\n")

    if (combinedErrors.isEmpty){
        Seq.empty
    } else {
      combinedErrors
    }
  }
}
