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

import base.SpecBase
import cats.data.ReaderT
import cats.implicits._
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import org.mockito.MockitoSugar
import repositories.SubmissionDetailsRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.xml.{Elem, NodeSeq}

class ManualSubmissionValidationEngineSpec extends SpecBase with MockitoSugar {

  val xsdError = "xsd-error"
  val defaultError = "There is a problem with this line number"
  val lineNumber = 0
  val noErrors: ListBuffer[SaxParseError] = ListBuffer()

  val addressError1 = SaxParseError(20, "cvc-minLength-valid: Value '' with length = '0' is " +
    "not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.")

  val enrolmentId = "123456"

  trait SetUp {
    val doesFileHaveBusinessErrors = false

    val mockXmlValidationService: XMLValidationService = mock[XMLValidationService]
    val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]

    val mockMetaDataValidationService: MetaDataValidationService = mock[MetaDataValidationService]

    val mockAuditService: AuditService = mock[AuditService]

    val mockBusinessRuleValidationService: BusinessRuleValidationService =
      new BusinessRuleValidationService(mockSubmissionDetailsRepository) {

        val dummyReader: ReaderT[Option, NodeSeq, Boolean] =
          ReaderT[Option, NodeSeq, Boolean](xml => {
            Some(!doesFileHaveBusinessErrors)
          })

        def dummyValidation(): ReaderT[Option, NodeSeq, Validation] = {
          for {
            result <- dummyReader
          } yield
            Validation(
              key = defaultError,
              value = result
            )
        }

        override def validateFile()(implicit hc: HeaderCarrier, ec: ExecutionContext): ReaderT[Option, NodeSeq, Future[Seq[Validation]]] = {
          for {
            v1 <- dummyValidation()
          } yield
            Future.successful(Seq(v1).filterNot(_.value))
        }

        override def extractDac6MetaData(): ReaderT[Option, NodeSeq, Dac6MetaData] = {
          for {
            _ <-  dummyReader
          }yield {
            Dac6MetaData("DAC6NEW", disclosureInformationPresent = true,
              initialDisclosureMA = false, messageRefId = "messageRefId")

          }
        }

      }

    val validationEngine = new ManualSubmissionValidationEngine(mockXmlValidationService,
      mockBusinessRuleValidationService,
      mockMetaDataValidationService,
      mockAuditService)

    val source = "src"
    val elem: Elem = <dummyElement>Test</dummyElement>
    val mockXML: Elem = <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
    val mockMetaData = Some(Dac6MetaData("DAC6NEW", disclosureInformationPresent = true,
      initialDisclosureMA = false, messageRefId = "messageRefId"))

  }
  "ValidateManualSubmission" - {
    "must return ManualSubmissionValidationSuccess when xml with no errors received" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)
      when(mockMetaDataValidationService.verifyMetaDataForManualSubmission(any(), any())(any(), any())).thenReturn(Future.successful(Right("id")))

      val xml = <dummyTag></dummyTag>

      Await.result(validationEngine.validateManualSubmission(xml, enrolmentId), 10 seconds) mustBe Some(ManualSubmissionValidationSuccess("id"))

      verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any(), any())(any())

    }

    "must return errors when xml with businessErrors received" in new SetUp {

      override val doesFileHaveBusinessErrors = true

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)
      when(mockMetaDataValidationService.verifyMetaDataForManualSubmission(any(), any())(any(), any())).thenReturn(Future.successful(Right("id")))

      val xml = <dummyTag></dummyTag>

      val expectedResult = Some(ManualSubmissionValidationFailure(Seq(defaultError)))
      Await.result(validationEngine.validateManualSubmission(xml, enrolmentId), 10 seconds) mustBe expectedResult
      verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any(), any())(any())

    }

    "must return errors when xml with metaData errors received" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)

      when(mockMetaDataValidationService.verifyMetaDataForManualSubmission(any(), any())(any(), any())).thenReturn(
        Future.successful(Left(Seq("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords"))))

      val xml = <dummyTag></dummyTag>

      val expectedResult = Some(ManualSubmissionValidationFailure(Seq("metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords")))
      Await.result(validationEngine.validateManualSubmission(xml, enrolmentId), 10 seconds) mustBe expectedResult
      verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any(), any())(any())

    }

    "must return none when xml parsing fails and audit failure" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(addressError1))
      when(mockMetaDataValidationService.verifyMetaDataForManualSubmission(any(), any())(any(), any())).thenReturn(Future.successful(Right("id")))

      val xml = <dummyTag></dummyTag>

      Await.result(validationEngine.validateManualSubmission(xml, enrolmentId), 10 seconds) mustBe None

      verify(mockAuditService, times(1)).auditManualSubmissionParseFailure(any(), any(), any())(any())
    }
  }
}
