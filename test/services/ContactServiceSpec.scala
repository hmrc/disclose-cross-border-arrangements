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

import models.UserRequest
import base.SpecBase
import connectors.SubscriptionConnector
import helpers.JsonFixtures.contactsResponse
import models.SubscriptionDetails
import models.subscription.{
  ContactInformationForIndividual,
  ContactInformationForOrganisation,
  DisplaySubscriptionForDACResponse,
  IndividualDetails,
  OrganisationDetails,
  PrimaryContact,
  ResponseCommon,
  ResponseDetail,
  SecondaryContact,
  SubscriptionForDACResponse
}
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.inject.bind
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContactServiceSpec extends SpecBase with BeforeAndAfterEach {

  override def beforeEach(): Unit = reset(mockSubscriptionConnector, mockSubscriptionCacheService)

  val mockSubscriptionConnector    = mock[SubscriptionConnector]
  val mockSubscriptionCacheService = mock[SubscriptionCacheService]

  "Contact Service Spec" - {
    val application = applicationBuilder()
      .overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[SubscriptionCacheService].toInstance(mockSubscriptionCacheService)
      )
      .build()

    "must correctly retrieve subscription from connector when not present in cache" in {
      val service = application.injector.instanceOf[ContactService]

      when(mockSubscriptionConnector.displaySubscriptionForDAC(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, contactsResponse)))

      when(mockSubscriptionCacheService.retrieveSubscriptionDetails(any())(any()))
        .thenReturn(Future.successful(None))

      val expectedSubscriptionDetails = SubscriptionDetails(
        "111111111",
        Some(""),
        true,
        ContactInformationForIndividual(IndividualDetails("First", "Last", None), "", Some(""), Some("")),
        Some(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None))
      )

      implicit val userRequest = UserRequest("", "", FakeRequest())

      val result = service.getLatestContacts("111111111")

      whenReady(result) {
        sub =>
          sub mustBe expectedSubscriptionDetails
          verify(mockSubscriptionCacheService, times(1)).retrieveSubscriptionDetails(any())(any())
          verify(mockSubscriptionConnector, times(1)).displaySubscriptionForDAC(any())(any(), any())
      }
    }

    "must correctly retrieve subscription when present in cache" in {
      val service = application.injector.instanceOf[ContactService]

      when(mockSubscriptionConnector.displaySubscriptionForDAC(any())(any(), any()))
        .thenReturn(Future.successful(HttpResponse(OK, contactsResponse)))

      when(mockSubscriptionCacheService.retrieveSubscriptionDetails(any())(any()))
        .thenReturn(
          Future.successful(
            Some(
              DisplaySubscriptionForDACResponse(
                SubscriptionForDACResponse(
                  ResponseCommon("", None, "", None),
                  ResponseDetail(
                    "111111111",
                    Some(""),
                    true,
                    PrimaryContact(Seq(ContactInformationForIndividual(IndividualDetails("First", "Last", None), "", Some(""), Some("")))),
                    Some(SecondaryContact(Seq(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None))))
                  )
                )
              )
            )
          )
        )

      val subscriptionDetails = SubscriptionDetails(
        "111111111",
        Some(""),
        true,
        ContactInformationForIndividual(IndividualDetails("First", "Last", None), "", Some(""), Some("")),
        Some(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None))
      )

      implicit val userRequest = UserRequest("", "", FakeRequest())

      val result = service.getLatestContacts("111111111")

      whenReady(result) {
        sub =>
          sub mustBe subscriptionDetails
          verify(mockSubscriptionCacheService, times(1)).retrieveSubscriptionDetails(any())(any())
          verify(mockSubscriptionConnector, times(0)).displaySubscriptionForDAC(any())(any(), any())
      }
    }
  }

}
