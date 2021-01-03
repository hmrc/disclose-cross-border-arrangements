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
import play.api.Configuration
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.IndexType
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionCacheRepository @Inject()(mongo: ReactiveMongoApi,
                                            config: Configuration)(implicit ec: ExecutionContext) {

  private val collectionName = "subscriptionCacheRepository"
  private val cacheTtl = config.get[Int]("mongodb.subscriptionCacheTTLInSeconds")

  private def collection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](collectionName))

  private val lastUpdatedIndex = IndexUtils.index(
    key     = Seq("lastUpdated" -> IndexType.Ascending),
    name    = Some("subscription-last-updated-index"),
    expireAfterSeconds = Some(cacheTtl)
  )

  val started: Future[Unit] =
    collection.flatMap {
      _.indexesManager.ensure(lastUpdatedIndex)
    }.map(_ => ())

  def get(id: String): Future[Option[CreateSubscriptionForDACRequest]] =
    collection.flatMap(_.find(Json.obj("_id" -> id), None).one[CreateSubscriptionForDACRequest])

  def set(id: String, subscription: CreateSubscriptionForDACRequest): Future[Boolean] = {

    val selector = Json.obj(
      "_id" -> id
    )

    val modifier = Json.obj(
      "$set" -> (subscription copy (lastUpdated = LocalDateTime.now))
    )

    collection.flatMap {
      _.update(ordered = false)
        .one(selector, modifier, upsert = true).map {
        lastError =>
          lastError.ok
      }
    }
  }
}

