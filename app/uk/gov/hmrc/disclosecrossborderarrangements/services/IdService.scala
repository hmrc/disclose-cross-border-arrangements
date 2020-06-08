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

  def doesArrangementIdExist(arrangementId: String): Future[Boolean] = Future(true)

}
