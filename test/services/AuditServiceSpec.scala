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
import models.{Dac6MetaData, SaxParseError}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, _}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class AuditServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {
  val auditConnector =  mock[AuditConnector]

  override def beforeEach() = {
    reset(auditConnector)
  }

  override lazy val app = new GuiceApplicationBuilder()
    .overrides(
      inject.bind[AuditConnector].toInstance(auditConnector)
    )
    .build()

  val auditService = app.injector.instanceOf[AuditService]

  val xml =
  <DAC6_Arrangement version="First" xmlns="urn:ukdac6:v0.1">
      <Header>
        <MessageRefId>GB0000000XXX</MessageRefId>
        <Timestamp>2020-05-14T17:10:00</Timestamp>
      </Header>
      <DAC6Disclosures>
        <DisclosureImportInstruction>DAC6NEW</DisclosureImportInstruction>
        <InitialDisclosureMA>false</InitialDisclosureMA>
      </DAC6Disclosures>
    </DAC6_Arrangement>

  val auditType = "disclosureFileSubmission"


  "Audit service must" - {
    "must generate correct payload for failed Manual Submission parsing" in {
      forAll(arbitrary[String], arbitrary[Option[String]], arbitrary[Option[String]], arbitrary[String]) { (enrolmentID, arrangementID, disclosureID, messageRefID) =>
        reset(auditConnector)

        when(auditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        val metaData = Dac6MetaData(importInstruction = "DAC6NEW",
          arrangementID = arrangementID,
          disclosureID = disclosureID,
          disclosureInformationPresent = true,
          initialDisclosureMA = true,
          messageRefId = messageRefID)

        val parseErrors = ListBuffer(SaxParseError(1, "error-message"), SaxParseError(2, "error-message2"))
        auditService.auditManualSubmissionParseFailure(enrolmentID, Some(metaData), parseErrors)

        val errorsArray =
          s"""|[
              |{
              |"lineNumber" : 1,
              |"errorMessage" : error-message
              |},{
              |"lineNumber" : 2,
              |"errorMessage" : error-message2
              |}
              |]""".stripMargin

        val arrangmentIdValue = arrangementID.getOrElse("None Provided")
        val disclosureIdValue = disclosureID.getOrElse("None Provided")

        val expectedjson = Json.obj(
          "enrolmentID" -> enrolmentID,
          "arrangementID" -> arrangmentIdValue,
          "disclosureID" -> disclosureIdValue,
          "messageRefID" -> metaData.messageRefId,
          "disclosureImportInstruction" -> "DAC6NEW",
          "initialDisclosureMA" -> "true",
          "errors" -> errorsArray)

        val eventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

        verify(auditConnector, times(1)).sendExtendedEvent(eventCaptor.capture())(any(), any())

        eventCaptor.getValue.detail mustBe expectedjson
      }
    }
  }
}


