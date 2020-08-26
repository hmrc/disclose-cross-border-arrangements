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

package services
import base.SpecBase
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, _}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditServiceSpec extends SpecBase with MockitoSugar {

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
        "send audit information with the correct value" in {

          val auditConnector =  mock[AuditConnector]

          val app = new GuiceApplicationBuilder()
            .overrides(
              inject.bind[AuditConnector].toInstance(auditConnector)
            )
            .build()

          val auditService = app.injector.instanceOf[AuditService]

          val argumentCaptorData: ArgumentCaptor[Map[String, String]] = ArgumentCaptor.forClass(classOf[Map[String, String]])

          doNothing().when(auditConnector).sendExplicitAudit(any[String], any[Map[String, String]])(any[HeaderCarrier], any[ExecutionContext])

          auditService.submissionAudit(xml, xml)

          verify(auditConnector, times(1))
            .sendExplicitAudit(any[String], argumentCaptorData.capture())(any[HeaderCarrier], any[ExecutionContext])

          assert(argumentCaptorData.getValue == ???)
        }
    }
}


