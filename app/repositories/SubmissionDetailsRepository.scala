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

import javax.inject.Inject
import models.SubmissionDetails
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.Cursor
import reactivemongo.api.ReadConcern.{Available, Local}
import reactivemongo.play.json.ImplicitBSONHandlers.JsObjectDocumentWriter
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.{ExecutionContext, Future}

class SubmissionDetailsRepository @Inject()(mongo: ReactiveMongoApi)
                                           (implicit ec: ExecutionContext) {

  private val collectionName: String = "submission-details"

  private def submissionDetailsCollection: Future[JSONCollection] =
    mongo.database.map(_.collection[JSONCollection](collectionName))

  //TODO: Not guaranteed to be unique - you could replace a file multiple times
  def getSubmissionDetails(disclosureID: String): Future[Option[SubmissionDetails]] =
    submissionDetailsCollection
      .flatMap(_.find(Json.obj("disclosureID" -> disclosureID), None)
        .one[SubmissionDetails]
      )

  //TODO: This should have optional paging to support many submissions
  def retrieveSubmissionHistory(enrolmentID: String): Future[List[SubmissionDetails]] = {
    val maxDocs = 10000
    val selector = Json.obj("enrolmentID" -> enrolmentID)
    val sortByLatestSubmission = Json.obj("submissionTime" -> -1)

    submissionDetailsCollection.flatMap(
      _.find(selector, None)
        .sort(sortByLatestSubmission)
        .cursor[SubmissionDetails]()
        .collect[List](maxDocs, Cursor.FailOnError())
    )
  }

  def countNoOfPreviousSubmissions(enrolmentID: String): Future[Long] = {
    val enrolmentSelector = Json.obj("enrolmentID" -> enrolmentID)
    submissionDetailsCollection.flatMap(
      _.count(
        selector = Option(enrolmentSelector),
        limit = Some(10),
        skip = 0,
        hint = None,
        readConcern = Local)
    )
  }

  def storeSubmissionDetails(submissionDetails: SubmissionDetails): Future[Boolean] = {
    submissionDetailsCollection.flatMap {
      _.insert(ordered = false)
        .one(submissionDetails).map { lastError =>
        lastError.ok
      }
    }
  }

}
