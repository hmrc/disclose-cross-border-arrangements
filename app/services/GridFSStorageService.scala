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

package services

import javax.inject.Inject
import play.api.libs.iteratee.Enumerator
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS}
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq


class GridFSStorageService @Inject()(mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext) {
  val gridFS: GridFS[BSONSerializationPack.type] = GridFS(mongo.mongoConnector.db())

  def writeFileToGridFS(fileName: String, enumerator: Enumerator[Array[Byte]]): Future[Boolean] = {

    val gridfsObj = DefaultFileToSave(
      filename = Some(fileName),
      contentType = Some("application/xml")
    )

    gridFS.save( enumerator, gridfsObj).map {
      _ => true
    }
  }

  def readFileFromGridFS(fileName: String): Future[Option[Array[Byte]]] = {
    //implicitly assumes uniqueness of filename TBD
    gridFS.find(BSONDocument("filename" -> fileName)).headOption.flatMap {
      case Some(readFile) =>  {
        val buf = new java.io.ByteArrayOutputStream()
        gridFS.readToOutputStream(readFile, buf).map{
          _ => Some(buf.toByteArray)
        }
      }
      case None => Future.successful(None)
    }

  }

}
