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
import controllers.Assets.OK
import controllers.auth.{FakeAuthAction, FakeIdentifierAuthAction, IdentifierAuthAction}
import generators.CacheModelGenerators
import models.subscription.cache.CreateSubscriptionForDACRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SubscriptionCacheService

import scala.concurrent.Future

class CacheControllerSpec extends SpecBase
  with ScalaCheckPropertyChecks
  with CacheModelGenerators
  with BeforeAndAfterEach {

  val mockSubscriptionCacheService = mock[SubscriptionCacheService]

  val application: Application =
    applicationBuilder()
      .overrides(
        bind[SubscriptionCacheService].toInstance(mockSubscriptionCacheService),
        bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
      ).build()


  "Cache Controller" - {
    "must store a subscription when given a valid create subscription payload" in {
     when(mockSubscriptionCacheService.storeSubscriptionDetails(any(), any()))
       .thenReturn(Future.successful(true))

     forAll(arbitrary[CreateSubscriptionForDACRequest]){
       (subscriptionRequest) =>
         val payload = Json.toJson(subscriptionRequest)
         val request = FakeRequest(POST, routes.CacheController.storeSubscriptionDetails().url)
           .withJsonBody(payload)

         val result: Future[Result] = route(application, request).value

         status(result) mustBe OK

     }
    }
  }

}
