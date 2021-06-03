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
import helpers.TestXml
import models.SaxParseError
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind

import java.net.URL
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import scala.collection.mutable.ListBuffer
import scala.io.Source

class XmlValidationServiceSpec extends SpecBase with TestXml {
  val noErrors : ListBuffer[SaxParseError] = ListBuffer()

  val application: Application = applicationBuilder()
    .overrides(
    bind[XMLValidatingParser].toInstance(MockitoSugar.mock[XMLValidatingParser])
  ).build()

  trait ActualSetup {

    //val testUrl: URL = getClass.getResource("/sitemap-v0.9.xsd")

    val schemaLang: String = javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
    val isoXsdUrl: URL = getClass.getResource("/schemas/IsoTypes_v1.01.xsd")
    val xsdUrl: URL = getClass.getResource("/schemas/UKDac6XSD_v0.5.xsd")

    val isoXsdStream: StreamSource = new StreamSource(isoXsdUrl.openStream())
    val ukDAC6XsdStream: StreamSource = new StreamSource(xsdUrl.openStream())

    val streams: Array[javax.xml.transform.Source] = Array(isoXsdStream, ukDAC6XsdStream)

    val schema: Schema = SchemaFactory.newInstance(schemaLang).newSchema(streams)

    val factory: SAXParserFactory = SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)
    factory.setSchema(schema)

    lazy val sut: XMLValidationService = {
      when(application.injector.instanceOf[XMLValidatingParser].validatingParser)
        .thenReturn(factory.newSAXParser())

      application.injector.instanceOf[XMLValidationService]
    }
  }

  "Validation Service" - {
    "must pass back errors if a file is invalid" in {
      val service = app.injector.instanceOf[XMLValidationService]

      val invalid = <this>
      <will>not validate</will>
      </this>

      val result = service.validateXml(invalid.mkString)

      result.isLeft mustBe true
    }

    "must correctly invalidate a submission with a data problem" in {
      val service = app.injector.instanceOf[XMLValidationService]
      val validsubmission =
        Source
          .fromInputStream(getClass.getResourceAsStream("/invalid.xml"))
          .getLines.mkString("\n")

      val result = service.validateXml(validsubmission)

      result.isLeft mustBe true
    }

    "must correctly validate a submission" in {
      val service = app.injector.instanceOf[XMLValidationService]
      val validsubmission =
        Source
          .fromInputStream(getClass.getResourceAsStream("/valid.xml"))
          .getLines.mkString("\n")

      val result = service.validateXml(validsubmission)

      result.isLeft mustBe false
    }

    "must return a ValidationSuccess with no errors for valid manual submission" in new ActualSetup {
      sut.validateManualSubmission(validXml) mustBe noErrors
    }

    "must return a ValidationFailure with errors for invalid manual submission" in new ActualSetup {

      val result = sut.validateManualSubmission(invalidXml)

      result.length mustBe 2

      result.head.lineNumber mustBe 20
      result.head.errorMessage mustBe "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'."
      result(1).lineNumber mustBe 20
      result(1).errorMessage mustBe "cvc-type.3.1.3: The value '' of element 'Street' is not valid."
    }
  }
}
