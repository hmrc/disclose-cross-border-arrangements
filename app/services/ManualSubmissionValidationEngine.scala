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

import models.{ManualSubmissionValidationFailure, ManualSubmissionValidationResult, ManualSubmissionValidationSuccess, SaxParseError}
import org.slf4j.LoggerFactory
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.{Elem, NodeSeq}

class ManualSubmissionValidationEngine @Inject()(xmlValidationService: XMLValidationService,
                                                 businessRuleValidationService: BusinessRuleValidationService,
                                                 metaDataValidationService: MetaDataValidationService,
                                                 auditService: AuditService) {

  private val logger = LoggerFactory.getLogger(getClass)
  private val noErrors: Seq[String] = Seq()


  def validateManualSubmission(xml: NodeSeq, enrolmentId: String)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[Option[ManualSubmissionValidationResult]] = {

    val elem = xml.asInstanceOf[Elem]

    try {
      val xmlAndXmlValidationStatus: ListBuffer[SaxParseError] = performXmlValidation(elem)
      val metaData = businessRuleValidationService.extractDac6MetaData()(elem)

      for {
        metaDateResult <- metaDataValidationService.verifyMetaDataForManualSubmission(metaData, enrolmentId)
        businessRulesResult <- performBusinessRulesValidation(elem)
      } yield {
        combineResults(xmlAndXmlValidationStatus, businessRulesResult, metaDateResult) match {

          case None =>  auditService.auditManualSubmissionParseFailure(elem, xmlAndXmlValidationStatus)
                         None
          case Some(Seq()) => Some(ManualSubmissionValidationSuccess(metaDateResult.right.getOrElse(metaData.fold("id")(_.messageRefId))))
          case Some(Seq(errors)) => Some(ManualSubmissionValidationFailure(Seq(errors)))
        }
      }
    } catch {
      case e: Exception =>
        logger.warn(s"XML validation failed. The XML parser has thrown the exception: $e")
        Future.successful(None)
    }
  }

  private def combineResults(xmlResult: ListBuffer[SaxParseError], businessRulesResult: Seq[String],
                             metaDataResult:  Either[Seq[String], String]):  Option[Seq[String]] = {

    if(xmlResult.isEmpty){
      if(metaDataResult.isLeft) {
        Some(businessRulesResult ++ metaDataResult.left.get)
      }else Some(businessRulesResult)
    }else None
  }

  def performXmlValidation(elem: Elem): ListBuffer[SaxParseError] = {

    xmlValidationService.validateManualSubmission(elem)

  }

  def performBusinessRulesValidation(elem: Elem)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[String]] = {

    businessRuleValidationService.validateFile()(hc, ec)(elem) match {
      case Some(value) => value.map {
        seqValidation =>
          seqValidation.map(_.key)

      }
      case None => Future.successful(noErrors)
    }

  }

  def performBusinessRulesValidationForMan(elem: Elem)
                                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[String]] = {

    businessRuleValidationService.validateFile()(hc, ec)(elem) match {
      case Some(value) => value.map {
        seqValidation =>
          seqValidation.map(_.key)

      }
      case None => Future(Seq())
    }

  }
}
