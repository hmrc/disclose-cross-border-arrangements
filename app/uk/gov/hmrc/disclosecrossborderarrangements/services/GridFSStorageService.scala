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

package uk.gov.hmrc.disclosecrossborderarrangements.services

import java.io.File

import javax.inject.Inject
import play.api.libs.iteratee.Enumerator
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS}
import reactivemongo.bson._

import scala.concurrent.ExecutionContext


class GridFSStorageService @Inject()(mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext) {


  def writeFileToGridFS(file: File) = {
    /*val gridFS: Future[GridFS[BSONSerializationPack.type]] = mongo.asyncGridFS

    gridFS.flatMap {
      gfs =>
        // Prepare the GridFS object to the file to be pushed
        val gridfsObj = DefaultFileToSave(
          filename = Some(file.getName),
          contentType = Some("application/xml")
        )

        gfs.save( Enumerator.fromFile(file), gridfsObj)
    }*/

    val gridFS: GridFS[BSONSerializationPack.type] = GridFS(mongo.mongoConnector.db())

    val gridfsObj = DefaultFileToSave(
      filename = Some(file.getName),
      contentType = Some("application/xml")
    )

    gridFS.save( Enumerator.fromFile(file), gridfsObj)
  }



}
