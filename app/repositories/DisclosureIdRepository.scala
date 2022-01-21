/*
 * Copyright 2022 HM Revenue & Customs
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

import models.DisclosureId
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object DisclosureIdRepository {

  def indexes = Seq(IndexModel(ascending("dateString"), IndexOptions().name("disclosure-id-date-index")))
}

class DisclosureIdRepository @Inject() (mongo: MongoComponent)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[DisclosureId](
      mongoComponent = mongo,
      collectionName = "disclosure-id",
      domainFormat = DisclosureId.format,
      indexes = DisclosureIdRepository.indexes,
      replaceIndexes = true
    ) {

  def doesDisclosureIdExist(disclosureId: DisclosureId): Future[Boolean] =
    collection
      .find(and(equal("dateString", disclosureId.dateString), equal("suffix", disclosureId.suffix)))
      .toFuture()
      .map(_.nonEmpty)

  def storeDisclosureId(disclosureId: DisclosureId): Future[DisclosureId] =
    collection
      .insertOne(disclosureId)
      .toFuture()
      .map(
        _ => disclosureId
      )
}
