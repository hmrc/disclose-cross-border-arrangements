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

package controllers

import base.SpecBase
import connectors.SubscriptionConnector
import controllers.Assets.OK
import controllers.auth.{FakeIdentifierAuthAction, IdentifierAuthAction}
import generators.CacheModelGenerators
import models.subscription._
import models.subscription.cache.CreateSubscriptionForDACRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SubscriptionCacheService
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class CacheControllerSpec extends SpecBase
  with CacheModelGenerators
  with BeforeAndAfterEach
  with ScalaCheckPropertyChecks {

  val mockSubscriptionCacheService: SubscriptionCacheService = mock[SubscriptionCacheService]
  val mockSubscriptionConnector: SubscriptionConnector = mock[SubscriptionConnector]

  val application: Application =
    applicationBuilder()
      .overrides(
        bind[SubscriptionCacheService].toInstance(mockSubscriptionCacheService),
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
      ).build()

  override def beforeEach(): Unit = {
    reset(mockSubscriptionCacheService)
    reset(mockSubscriptionConnector)
  }

  val errorStatusCodes: Seq[Int] = Seq(
    BAD_REQUEST, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED,
    CONFLICT, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE
  )

  "Cache Controller" - {
    "must store a subscription when given a valid create subscription payload" in {
     when(mockSubscriptionCacheService.storeSubscriptionDetails(any(), any()))
       .thenReturn(Future.successful(true))

     forAll(arbitrary[CreateSubscriptionForDACRequest]){
       subscriptionRequest =>
         val payload = Json.toJson(subscriptionRequest)
         val request = FakeRequest(POST, routes.CacheController.storeSubscriptionDetails().url)
           .withJsonBody(payload)

         val result: Future[Result] = route(application, request).value

         status(result) mustBe OK

     }
    }

    "must retrieve a subscription from the cache where one exists" in {
      when(mockSubscriptionCacheService.retrieveSubscriptionDetails(any())(any()))
        .thenReturn(Future.successful(Some(DisplaySubscriptionForDACResponse(
          SubscriptionForDACResponse(
            ResponseCommon("", None, "", None),
            ResponseDetail("111111111", Some(""),
              isGBUser = true,
              PrimaryContact(Seq(ContactInformationForIndividual(IndividualDetails("First", "Last", None), "", Some(""), Some("")))),
              Some(SecondaryContact(Seq(ContactInformationForOrganisation(OrganisationDetails(""), "", None, None)))))
          )))))

      forAll(arbitrary[DisplaySubscriptionForDACRequest]) {
        display =>
          val payload = Json.toJson(display)

          val request = FakeRequest(POST, routes.CacheController.retrieveSubscription().url)
            .withJsonBody(payload)

          val result: Future[Result] = route(application, request).value

          status(result) mustBe OK
          //verify(mockSubscriptionCacheService, times(1)).retrieveSubscriptionDetails(any())(any())
          verify(mockSubscriptionConnector, times(0)).displaySubscriptionForDAC(any())(any(), any())
      }
    }

    "must retrieve a subscription from the hod where one does not exist in cache" in {

      forAll(arbitrary[DisplaySubscriptionForDACRequest], Gen.oneOf(errorStatusCodes)) {
        (display, statusCodes) =>
          when(mockSubscriptionCacheService.retrieveSubscriptionDetails(any())(any()))
            .thenReturn(Future.successful(None))
          when(mockSubscriptionConnector.displaySubscriptionForDAC(any())(any(), any()))
            .thenReturn(Future.successful(HttpResponse(statusCodes, "")))

          val payload = Json.toJson(display)
          val request = FakeRequest(POST, routes.CacheController.retrieveSubscription().url)
            .withJsonBody(payload)

          val result: Future[Result] = route(application, request).value

          status(result) mustBe statusCodes
      }
    }
  }

}
