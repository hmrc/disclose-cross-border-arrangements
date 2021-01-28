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
import models.SaxParseError
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, _}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

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
//       "must generate correct payload for failed Manual Submission parsing" in {
//          when(auditConnector.sendExtendedEvent(any())(any(), any()))
//            .thenReturn(Future.successful(AuditResult.Success))
//
//          val xml = <dummyTag></dummyTag>
//          val parseErrors = ListBuffer(SaxParseError(1, "errorMessage"))
//          auditService.auditManualSubmissionParseFailure(xml, parseErrors)
//
//          val expectedjson = Json.obj("xml" -> xml.toString(),
//            "errors" -> parseErrors.toString())
//
//          val eventCaptor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
//
//          verify(auditConnector, times(1)).sendExtendedEvent(eventCaptor.capture())(any(),any())
//
//          eventCaptor.getValue.detail mustBe expectedjson
//      }
  }
}


