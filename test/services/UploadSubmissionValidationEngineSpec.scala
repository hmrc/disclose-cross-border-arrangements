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
import helpers.{ErrorMessageHelper, XmlErrorMessageHelper}
import models._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import repositories.SubmissionDetailsRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.xml.{Elem, NodeSeq}

class UploadSubmissionValidationEngineSpec extends SpecBase with MockitoSugar {

  val xsdError = "xsd-error"
  val defaultError = "There is a problem with this line number"
  val lineNumber = 0
  val noErrors: ListBuffer[SaxParseError] = ListBuffer()

  val addressError1 = SaxParseError(20, "cvc-minLength-valid: Value '' with length = '0' is " +
    "not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.")

  val addressError2 = SaxParseError(20, "cvc-type.3.1.3: The value '' of element 'Street' is not valid.")

  val over400 = "a" * 401
  val over4000 = "a" * 4001

  val maxLengthError1 = SaxParseError(116, s"cvc-maxLength-valid: Value '$over400' with length = '401' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max400_Type'.")
  val maxlengthError2 = SaxParseError(116, s"cvc-type.3.1.3: The value '$over400' of element 'BuildingIdentifier' is not valid.")

  val maxLengthError3 = SaxParseError(116, s"cvc-maxLength-valid: Value '$over4000' with length = '4001' is not facet-valid with respect to maxLength '4000' for type 'StringMin1Max4000_Type'.")
  val maxlengthError4 = SaxParseError(116, s"cvc-type.3.1.3: The value '$over4000' of element 'NationalProvision' is not valid.")

  val countryCodeError1 = SaxParseError(123,"cvc-enumeration-valid: Value 'Invalid code' is not facet-valid with respect to enumeration '[AF, AX, AL, DZ]'. It must be a value from the enumeration.")
  val countryCodeError2 = SaxParseError(123, "cvc-type.3.1.3: The value 'Raneevev' of element 'Country' is not valid.")

  val concernedMsError1 = SaxParseError(177,"cvc-enumeration-valid: Value 'CdvvdvZ' is not facet-valid with respect to enumeration '[AT, SE, GB]'. It must be a value from the enumeration.")
  val concernedMsError2 = SaxParseError(177,"cvc-type.3.1.3: The value 'CdvvdvZ' of element 'ConcernedMS' is not valid.")


  val countryExemptionError1 = SaxParseError(133,"cvc-enumeration-valid: Value 'eevev' is not facet-valid with respect to enumeration '[AF, VE, VN, VG, VI, WF, EH, YE, ZM, ZW, XK, XX]'. It must be a value from the enumeration.")
  val countryExemptionError2 = SaxParseError(133,"cvc-type.3.1.3: The value 'eevev' of element 'CountryExemption' is not valid.")

  val reasonError1 = SaxParseError(169,"cvc-enumeration-valid: Value 'DAC670vdvdvd4' is not facet-valid with respect to enumeration '[DAC6701, DAC6702, DAC6703, DAC6704]'. It must be a value from the enumeration.")
  val reasonError2 = SaxParseError(169,"cvc-type.3.1.3: The value 'DAC670vdvdvd4' of element 'Reason' is not valid.")

  val intermediaryCapacityError1 = SaxParseError(129,"cvc-enumeration-valid: Value 'DAC61102fefef' is not facet-valid with respect to enumeration '[DAC61101, DAC61102]'. It must be a value from the enumeration.")
  val intermediaryCapacityError2 = SaxParseError(129,"cvc-type.3.1.3: The value 'DAC61102fefef' of element 'Capacity' is not valid.")

  val relevantTpDiscloserCapacityError1 = SaxParseError(37,"cvc-enumeration-valid: Value 'DAC61105hhh' is not facet-valid with respect to enumeration '[DAC61104, DAC61105, DAC61106]'. It must be a value from the enumeration.")
  val relevantTpDiscloserCapacityError2 = SaxParseError(37,"cvc-type.3.1.3: The value 'DAC61105hhh' of element 'Capacity' is not valid.")

  val missingAddressErrors = ListBuffer(addressError1, addressError2)

  val cityError1 = SaxParseError(27, "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'.")
  val cityError2 = SaxParseError(27, "cvc-type.3.1.3: The value '' of element 'City' is not valid.")
  val missingCityErrors = ListBuffer(cityError1, cityError2)

  val invalidAttributeCodeError = SaxParseError(175,"cvc-attribute.3: The value 'VUVs' of attribute 'currCode' on element 'Amount' is not valid with respect to its type, 'currCode_Type'.")

  val issuedByError1 = SaxParseError(18,"cvc-enumeration-valid: Value 'GBf' is not facet-valid with respect to enumeration '[AF, AX]'. It must be a value from the enumeration.")
  val issuedByError2 = SaxParseError(18,"cvc-attribute.3: The value 'GBf' of attribute 'issuedBy' on element 'TIN' is not valid with respect to its type, 'CountryCode_Type'.")

  val enrolmentId = "123456"

  trait SetUp {
    val doesFileHaveBusinessErrors = false

    val mockXmlValidationService: XMLValidationService = mock[XMLValidationService]
    val mockSubmissionDetailsRepository: SubmissionDetailsRepository = mock[SubmissionDetailsRepository]
    val mockMetaDataValidationService: MetaDataValidationService = mock[MetaDataValidationService]
    val mockErrorMessageHelper: ErrorMessageHelper = new ErrorMessageHelper
    val mockXmlErrorMessageHelper: XmlErrorMessageHelper = new XmlErrorMessageHelper
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

    val validationEngine = new UploadSubmissionValidationEngine(mockXmlValidationService,
      mockBusinessRuleValidationService,
      mockMetaDataValidationService,
      mockXmlErrorMessageHelper,
      mockErrorMessageHelper,
      mockAuditService)

    val source = "src"
    val elem: Elem = <dummyElement>Test</dummyElement>
    val mockXML: Elem = <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
    val mockMetaData = Dac6MetaData("DAC6NEW", disclosureInformationPresent = true,
      initialDisclosureMA = false, messageRefId = "messageRefId")

  }

  //TODO - fix auditing & fix final test

  "ValidateUploadSubmission" - {

//    "must return ValidationSuccess for valid file" in new SetUp {
//      when(mockXmlValidationService.validateXml(any())).thenReturn(Left(noErrors))
//      when(mockMetaDataValidationService.verifyMetaData(any(), any())(any(), any())).thenReturn(Future.successful(Seq()))
//      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds) mustBe Right(UploadSubmissionValidationSuccess(mockMetaData))
//    }

    "must return UploadSubmissionValidationSuccess when xml with no errors received" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds) mustBe Some(UploadSubmissionValidationSuccess(mockMetaData))

//      verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any(), any())(any())

    }

    "must return errors when xml with businessErrors received" in new SetUp {

      override val doesFileHaveBusinessErrors = true

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedResult = Some(UploadSubmissionValidationFailure(Seq(GenericError(lineNumber, defaultError))))
      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds) mustBe expectedResult
//      verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any(), any())(any()) //TODO - add auditing

    }

    "must return errors when xml with metaData errors received" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)

      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(
        Future.successful(Left(Seq(GenericError(lineNumber, "metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords")))))

      val expectedResult = Some(UploadSubmissionValidationFailure(Seq(GenericError(lineNumber, "metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords"))))
      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds) mustBe expectedResult
//      verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any(), any())(any()) /TODO - add auditing

    }

    "must return ValidationFailure for file which multiple pieces of mandatory information missing" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(addressError1, addressError2, cityError1, cityError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(20, "Enter a Street"), GenericError(27, "Enter a City"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds) mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(2)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for valid file which fails metaDataCheck and audit outcome" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(
        Future(Left(Seq(GenericError(1, "metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords")))))

      val expectedResult = Some(UploadSubmissionValidationFailure(Seq(GenericError(1, "metaDataRules.arrangementId.arrangementIdDoesNotMatchRecords"))))
      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds) mustBe expectedResult
//      verify(mockAuditService, times(0)).auditManualSubmissionParseFailure(any(), any(), any())(any())
    }

    "must return ValidationFailure for file missing mandatory attributes" in new SetUp {

      val missingAttributeError: SaxParseError = SaxParseError(175, "cvc-complex-type.4: Attribute 'currCode' must appear on element 'Amount'.")

      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(missingAttributeError))

      val expectedErrors = Seq(GenericError(175, "Enter an Amount currCode"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for file where element is too long (1-400 allowed)" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(maxLengthError1, maxlengthError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(116, "BuildingIdentifier must be 400 characters or less"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for file where element is too long (1-4000 allowed)" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(maxLengthError3, maxlengthError4))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(116, "NationalProvision must be 4000 characters or less"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for file with invalid country code" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(countryCodeError1, countryCodeError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(123, "Country is not one of the ISO country codes"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for file with invalid countryMS code" in new SetUp {
      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(concernedMsError1, concernedMsError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(177, "ConcernedMS is not one of the ISO EU Member State country codes"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for file with invalid countryExemption code" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(countryExemptionError1, countryExemptionError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(133, "CountryExemption is not one of the ISO country codes"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())
    }

    "must return ValidationFailure for file with invalid Reason entry code" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(reasonError1, reasonError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(169, "Reason is not one of the allowed values"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for file with invalid Intermediary Capacity code" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(intermediaryCapacityError1, intermediaryCapacityError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(129, "Capacity is not one of the allowed values (DAC61101, DAC61102) for Intermediary"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }


    "must return ValidationFailure for file with invalid RelevantTaxpayer Discloser Capacity code" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(relevantTpDiscloserCapacityError1, relevantTpDiscloserCapacityError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(37, "Capacity is not one of the allowed values (DAC61104, DAC61105, DAC61106) for Taxpayer"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for file with invalid issuedBy code" in new SetUp {

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(issuedByError1, issuedByError2))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(18, "TIN issuedBy is not one of the ISO country codes"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure with generic error message if parse error is not in an expected format" in new SetUp {

      val randomParseError: SaxParseError = SaxParseError(lineNumber, xsdError)
      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(randomParseError))
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(lineNumber, "There is a problem with this line number"))

      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(1)).auditErrorMessage(any())(any())

    }

    "must return ValidationFailure for file which fails business rules validation" in new SetUp {
      override val doesFileHaveBusinessErrors = true

      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))
      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(noErrors)

      val expectedErrors = Seq(GenericError(lineNumber, defaultError))
      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
    }

    "must return a ValidationFailure with a combined list of errors for a for file which " +
      "fails both xsd checks and business rules validation and order errors correctly" in new SetUp {
      override val doesFileHaveBusinessErrors = true

      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(missingAddressErrors)
      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))

      val expectedErrors = Seq(GenericError(lineNumber, defaultError), GenericError(20, "Enter a Street"))
      Await.result(validationEngine.validateUploadSubmission(elem, enrolmentId), 10 seconds)  mustBe Some(UploadSubmissionValidationFailure(expectedErrors))
//      verify(mockAuditService, times(1)).auditValidationFailure(any(), any(), any())(any())
//      verify(mockAuditService, times(2)).auditErrorMessage(any())(any())
    }

//    "must return none when xml parsing fails and audit failure" in new SetUp {
//
//      when(mockXmlValidationService.validateManualSubmission(any())).thenReturn(ListBuffer(addressError1))
//      when(mockMetaDataValidationService.verifyMetaDataForUploadSubmission(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(mockMetaData)))
//
//      val xml = <dummyTag></dummyTag>
//
//      Await.result(validationEngine.validateUploadSubmission(xml, enrolmentId), 10 seconds) mustBe None

//      verify(mockAuditService, times(1)).auditManualSubmissionParseFailure(any(), any(), any())(any())
//    }

//    "must throw an exception if XML parser throws an exception (e.g. missing closing tags)" in new SetUp {
//      val exception = new RuntimeException
//      when(mockXmlValidationService.validateXml(any())).thenThrow(exception)
//
//      Await.result(validationEngine.validateFile(source, enrolmentId, businessRulesCheckRequired = false), 10 seconds) mustBe Left(exception)
//    }
  }
}