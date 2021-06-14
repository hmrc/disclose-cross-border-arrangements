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
import generators.CacheModelGenerators
import models.subscription.cache.CreateSubscriptionForDACRequest
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.inject.bind
import repositories.SubscriptionCacheRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionCacheServiceSpec extends SpecBase
  with BeforeAndAfterEach
  with CacheModelGenerators
  with ScalaCheckPropertyChecks {

  val mockCacheRepository: SubscriptionCacheRepository = mock[SubscriptionCacheRepository]

  val application = applicationBuilder()
    .overrides(
      bind[SubscriptionCacheRepository].toInstance(mockCacheRepository)
    )
    .build()

  "Subscription service spec" - {
    "must retrieve a cached response from the repository and return a faked hod response" in {
      forAll(arbitrary[CreateSubscriptionForDACRequest]) {
        create =>
        when(mockCacheRepository.get(any())).thenReturn(Future.successful(Some(create)))

        val service = application.injector.instanceOf[SubscriptionCacheService]
        val result = service.retrieveSubscriptionDetails("myid")

        result.futureValue.isDefined mustBe true
      }
    }

    "must return None when cache does not return a hit" in {
      when(mockCacheRepository.get(any())).thenReturn(Future.successful(None))

      val service = application.injector.instanceOf[SubscriptionCacheService]
      val result = service.retrieveSubscriptionDetails("myid")

      result.futureValue.isDefined mustBe false
    }

    "must set a cached response in repository" in {
      when(mockCacheRepository.set(any(), any())).thenReturn(Future.successful(true))
      forAll(arbitrary[CreateSubscriptionForDACRequest]) {
        create =>
          val service = application.injector.instanceOf[SubscriptionCacheService]
          val result = service.storeSubscriptionDetails("myid", create)

          result.futureValue mustBe true
      }
    }
  }

}
