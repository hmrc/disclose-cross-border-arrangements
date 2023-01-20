/*
 * Copyright 2023 HM Revenue & Customs
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

import models.ArrangementId
import org.mongodb.scala.model.Filters.and
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import javax.inject.Singleton

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object ArrangementIdRepository {

  def indexes = Seq(IndexModel(ascending("dateString"), IndexOptions().name("arrangement-id-date-index")))
}

@Singleton
class ArrangementIdRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ArrangementId](
      mongoComponent = mongo,
      collectionName = "arrangement-id",
      domainFormat = ArrangementId.format,
      indexes = ArrangementIdRepository.indexes,
      replaceIndexes = true
    ) {

  def doesArrangementIdExist(arrangementId: ArrangementId): Future[Boolean] =
    collection
      .find(and(equal("dateString", arrangementId.dateString), equal("suffix", arrangementId.suffix)))
      .toFuture()
      .map(_.nonEmpty)

  def storeArrangementId(arrangementId: ArrangementId): Future[ArrangementId] =
    collection
      .insertOne(arrangementId)
      .toFuture()
      .map(
        _ => arrangementId
      )
}
