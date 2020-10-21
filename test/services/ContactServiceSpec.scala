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
import connectors.SubscriptionConnector
import helpers.JsonFixtures.contactsResponse
import models.{ContactInformationForIndividual, ContactInformationForOrganisation, IndividualDetails, OrganisationDetails, SubscriptionDetails}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import play.api.inject.bind
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactServiceSpec extends SpecBase
  with MockitoSugar {

  val mockSubscriptionConnector = mock[SubscriptionConnector]

  "Contact Service Spec" - {
    val application = applicationBuilder()
      .overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      )
      .build()

    "must correctly retrieve subscription" in {
      val service = application.injector.instanceOf[ContactService]

      when(mockSubscriptionConnector.displaySubscriptionForDAC(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, contactsResponse)))


      val expectedSubscriptionDetails = SubscriptionDetails("111111111",
        Some(""),
        true,
        ContactInformationForIndividual(IndividualDetails("First", "Last", None), "", Some(""), Some("")),
        Some(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None)))

      val result = service.getLatestContacts("111111111")

      whenReady(result) {
        sub => sub mustBe expectedSubscriptionDetails
      }
    }
  }

}
