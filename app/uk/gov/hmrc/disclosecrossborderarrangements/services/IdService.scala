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

import javax.inject.Inject
import uk.gov.hmrc.disclosecrossborderarrangements.helpers.{DateHelper, SuffixHelper}
import uk.gov.hmrc.disclosecrossborderarrangements.models.{ArrangementId, DisclosureId}
import uk.gov.hmrc.disclosecrossborderarrangements.repositories.{ArrangementIdRepository, DisclosureIdRepository}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class IdService @Inject()(val dateHelper: DateHelper,
                          val suffixHelper: SuffixHelper,
                          arrangementIdRepository: ArrangementIdRepository,
                          disclosureIdRepository: DisclosureIdRepository){

  def date : String = dateHelper.today.toString("YYYYMMdd")

  val arrangementIdRegEx = "[A-Z]{2}[A]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"

  def generateArrangementId(): Future[ArrangementId] = {

    val newArrangementId =  ArrangementId(dateString = date, suffix = suffixHelper.generateSuffix())

    arrangementIdRepository.doesArrangementIdExist(newArrangementId).flatMap {
      case true =>  generateArrangementId()
      case false => arrangementIdRepository.storeArrangementId(newArrangementId)
    }
  }

  def generateDisclosureId(): Future[DisclosureId] = {

    val newDisclosureId =  DisclosureId(dateString = date, suffix = suffixHelper.generateSuffix())

    disclosureIdRepository.doesDisclosureIdExist(newDisclosureId).flatMap {
      case true =>  generateDisclosureId()
      case false => disclosureIdRepository.storeDisclosureId(newDisclosureId)
    }
  }

  def verifyArrangementId(suppliedArrangementId: String): Future[Option[Boolean]] =

    createArrangementIdFromSuppliedString(suppliedArrangementId) match {
      case Some(validArrangementId) => arrangementIdRepository.doesArrangementIdExist(validArrangementId).map(
                                       result => Some(result))
      case None => Future(None)
    }


  def createArrangementIdFromSuppliedString(suppliedString: String): Option[ArrangementId] = {

    if(suppliedString.matches(arrangementIdRegEx)) {
    Some(ArrangementId(prefix = suppliedString.substring(0,3),
                       dateString = suppliedString.substring(3, 11),
                       suffix = suppliedString.substring(11)))
    }else None

  }
}
