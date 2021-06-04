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

package repositories

import models.subscription.cache.CreateSubscriptionForDACRequest
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions, ReplaceOptions}
import play.api.Configuration
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object SubscriptionCacheRepository {

  def cacheTtl(config: Configuration): Int = config.get[Int]("mongodb.subscriptionCacheTTLInSeconds")

  def indexes(config: Configuration) = Seq(IndexModel(ascending("lastUpdated")
    , IndexOptions().name("subscription-last-updated-index").expireAfter(cacheTtl(config), TimeUnit.SECONDS) ))
}

class SubscriptionCacheRepository @Inject()(mongo: MongoComponent, config: Configuration)(implicit ec: ExecutionContext
) extends PlayMongoRepository[CreateSubscriptionForDACRequest] (
  mongoComponent = mongo,
  collectionName = "subscriptionCacheRepository",
  domainFormat   = CreateSubscriptionForDACRequest.format,
  indexes        = SubscriptionCacheRepository.indexes(config),
  replaceIndexes = true
) {

  def get(id: String): Future[Option[CreateSubscriptionForDACRequest]] =
    collection.find(equal("_id", id)).first().toFutureOption()

  def set(id: String, subscription: CreateSubscriptionForDACRequest): Future[Boolean] = {

    val filter = equal("_id", id)
    val data = subscription copy (lastUpdated = LocalDateTime.now)
    val options = ReplaceOptions().upsert(true)

    collection.replaceOne(filter, data, options).toFuture.map(_ => true)
  }
}
