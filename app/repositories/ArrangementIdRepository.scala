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

package repositories

import akka.stream.Materializer
import javax.inject.Inject
import models.ArrangementId
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.bson.collection.BSONSerializationPack
import reactivemongo.api.indexes.Index.Aux
import reactivemongo.api.indexes.IndexType
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class ArrangementIdRepository @Inject()(mongo: ReactiveMongoApi
                                       )(implicit ec: ExecutionContext, m: Materializer) {

  private val arrangementIdCollectionName: String = "arrangement-id"

  private val dateIndex: Aux[BSONSerializationPack.type] = IndexUtils.index(
    key     = Seq("dateString" -> IndexType.Ascending),
    name    = Some("arrangement-id-date-index"),
    expireAfterSeconds = None,
    unique = true
  )

  lazy val ensureIndexes: Future[Unit] =
    for {
      collection <- mongo.database.map(_.collection[JSONCollection](arrangementIdCollectionName))
      _ <- collection.indexesManager.ensure(dateIndex)
    } yield ()

  private def arrangementIdCollection: Future[JSONCollection] =
    for {
      _ <- ensureIndexes
      collection <- mongo.database.map(_.collection[JSONCollection](arrangementIdCollectionName))
    } yield collection

  def doesArrangementIdExist(arrangementId: ArrangementId):Future[Boolean] = {
    val selector = Json.obj(
      "dateString" -> arrangementId.dateString,
      "suffix" -> arrangementId.suffix
    )

    arrangementIdCollection.flatMap {
      _.find[JsObject, ArrangementId](selector, None).one[ArrangementId]
    } map(_.isDefined)
  }

  def storeArrangementId(arrangementId: ArrangementId): Future[ArrangementId] =
    arrangementIdCollection.flatMap {
      _.insert(ordered = false)
        .one(arrangementId).map {
        lastError =>
          lastError.ok
          arrangementId
      }
    }

}
