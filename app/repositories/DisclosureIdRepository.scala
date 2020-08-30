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
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import models.DisclosureId

import scala.concurrent.{ExecutionContext, Future}

class DisclosureIdRepository @Inject()(mongo: ReactiveMongoApi
                                      )(implicit ec: ExecutionContext, m: Materializer) {


  private val disclosureIdCollectionName: String = "disclosure-id"

  private val dateIndex = IndexUtils.index(
    key     = Seq("dateString" -> IndexType.Ascending),
    name    = Some("disclosure-id-date-index"),
    expireAfterSeconds = None
  )

  lazy val ensureIndex: Future[Unit] =
    for {
      collection <- mongo.database.map(_.collection[JSONCollection](disclosureIdCollectionName))
      _ <- collection.indexesManager.ensure(dateIndex)
    } yield ()

  private def disclosureIdCollection: Future[JSONCollection] =
    for {
      _ <- ensureIndex
      collection <- mongo.database.map(_.collection[JSONCollection](disclosureIdCollectionName))
    } yield collection

  def doesDisclosureIdExist(disclosureId: DisclosureId): Future[Boolean] = {
    val selector = Json.obj(
      "dateString" -> disclosureId.dateString,
      "suffix" -> disclosureId.suffix
    )

    disclosureIdCollection.flatMap(
      _.find[JsObject, DisclosureId](selector, None).one[DisclosureId]
    ) map (_.isDefined)
  }

  def storeDisclosureId(disclosureId: DisclosureId): Future[DisclosureId] =
    disclosureIdCollection.flatMap {
      _.insert(ordered = false)
        .one(disclosureId).map {
        lastError =>
          lastError.ok
          disclosureId
      }
    }

}
