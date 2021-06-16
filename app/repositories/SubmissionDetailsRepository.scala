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

import models.SubmissionDetails
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal, or, regex}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

object SubmissionDetailsRepository {

  def indexes = Seq(IndexModel(ascending("submissionTime")
    , IndexOptions().name("submission-time-ttl-index").expireAfter(6*365, TimeUnit.DAYS) ))
}

class SubmissionDetailsRepository @Inject()(mongo: MongoComponent)(implicit ec: ExecutionContext
) extends PlayMongoRepository[SubmissionDetails] (
  mongoComponent = mongo,
  collectionName = "submission-details",
  domainFormat   = SubmissionDetails.format,
  indexes        = SubmissionDetailsRepository.indexes,
  replaceIndexes = true
) {


  //TODO: Not guaranteed to be unique - you could replace a file multiple times
  def getSubmissionDetails(disclosureID: String): Future[Option[SubmissionDetails]] =
    collection.find(equal("disclosureID", disclosureID)).first().toFutureOption()

  def retrieveFirstDisclosureForArrangementId(arrangementID: String): Future[Option[SubmissionDetails]] =
    collection.find( and( equal("arrangementID", arrangementID), equal("importInstruction", "New")))
      .first().toFutureOption()

  val sortByLatestSubmission: Bson =
    orderBy(descending("submissionTime"), ascending("arrangementID", "disclosureID"))

  def retrieveSubmissionHistory(enrolmentID: String): Future[Seq[SubmissionDetails]] = {
    val maxDocs = 10000
    collection.find(equal("enrolmentID", enrolmentID))
      .limit(maxDocs)
      .sort(sortByLatestSubmission)
      .toFuture()
  }

  def countNoOfPreviousSubmissions(enrolmentID: String): Future[Long] =
    collection.countDocuments(equal("enrolmentID", enrolmentID)).toFuture()

  def storeSubmissionDetails(submissionDetails: SubmissionDetails): Future[Boolean] =
    collection.insertOne(submissionDetails).toFuture().map(_ => true)

  def searchSubmissions(searchCriteria: String): Future[Seq[SubmissionDetails]] = {
    val pattern = s"$searchCriteria.*"
    val maxDocs = 50
    val caseInsensitiveOption = "i"
    collection.find( or(
      regex("arrangementID", pattern, caseInsensitiveOption)
      , regex("disclosureID", pattern, caseInsensitiveOption)
      , regex("messageRefId", pattern, caseInsensitiveOption)
    )).limit(maxDocs)
      .sort(sortByLatestSubmission)
      .toFuture()
  }

  def doesDisclosureIdMatchEnrolmentID(disclosureID: String, enrolmentID: String): Future[Boolean] =
    collection.find( and( equal("enrolmentID", enrolmentID), equal("disclosureID", disclosureID)))
      .sort(descending("submissionTime"))
      .first()
      .toFutureOption()
      .map(_.isDefined)

  def doesDisclosureIdMatchArrangementID(disclosureID: String, arrangementID: String): Future[Boolean] =
    collection.find( and( equal("arrangementID", arrangementID), equal("disclosureID", disclosureID)))
      .sort(descending("submissionTime"))
      .first()
      .toFutureOption()
      .map(_.isDefined)

}
