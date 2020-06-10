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
import models.ArrangementId

import scala.concurrent.{ExecutionContext, Future}

class ArrangementIdRepository @Inject()(mongo: ReactiveMongoApi
                                       )(implicit ec: ExecutionContext, m: Materializer) {


  private val arrangementIdCollectionName: String = "arrangement-id"

  private def arrangementIdCollection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](arrangementIdCollectionName))

  private val dateIndex = Index(
    key     = Seq("dateString" -> IndexType.Ascending),
    name    = Some("arrangement-id-date-index")
  )

  val started: Future[Unit] =
    arrangementIdCollection.flatMap {
      _.indexesManager.ensure(dateIndex)
    }.map(_ => ())


  def doesArrangementIdExist(arrangementId: ArrangementId):Future[Boolean] = {
    arrangementIdCollection.flatMap(
      _.find(Json.obj("dateString" -> arrangementId.dateString, "suffix" -> arrangementId.suffix), None).one[ArrangementId]).map{
      case None => false
      case _ => true
    }
  }

  def storeArrangementId(arrangementId: ArrangementId): Future[ArrangementId] = {

    arrangementIdCollection.flatMap {
      _.insert(ordered = false)
        .one(arrangementId).map {
        lastError =>
          lastError.ok
          arrangementId
      }
    }
  }
}

