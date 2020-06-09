package uk.gov.hmrc.disclosecrossborderarrangements.services

import java.io.File

import javax.inject.Inject
import play.api.libs.iteratee.Enumerator
import reactivemongo.api.BSONSerializationPack
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS}
import reactivemongo.bson._
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.{ExecutionContext, Future}


class GridFSStorageService @Inject()(mongo: ReactiveMongoApi)(implicit ec: ExecutionContext) {


  def writeFileToGridFS(file: File) = {
    val gridFS: Future[GridFS[BSONSerializationPack.type]] = mongo.asyncGridFS

    gridFS.flatMap {
      gfs =>
        // Prepare the GridFS object to the file to be pushed
        val gridfsObj = DefaultFileToSave(
          filename = Some(file.getName),
          contentType = Some("application/xml")
        )

        gfs.save( Enumerator.fromFile(file), gridfsObj)
    }

  }



}
