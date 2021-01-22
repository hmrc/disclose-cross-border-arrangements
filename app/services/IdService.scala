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

package services

import helpers.{DateHelper, SuffixHelper}
import models.{ArrangementId, DisclosureId}
import repositories.{ArrangementIdRepository, DisclosureIdRepository, SubmissionDetailsRepository}

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdService @Inject()(val dateHelper: DateHelper,
                          val suffixHelper: SuffixHelper,
                          arrangementIdRepository: ArrangementIdRepository,
                          disclosureIdRepository: DisclosureIdRepository,
                          submissionDetailsRepository: SubmissionDetailsRepository){

  def date : String = dateHelper.today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

  val arrangementIdRegEx = "[A-Z]{2}[A]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"
  val disclosureIdRegEx = "[A-Z]{2}[D]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})"

  val nonUkPrefixes = List("ATA", "BEA", "BGA", "CYA", "CZA", "DKA", "EEA", "FIA", "FRA", "DEA", "GRA", "HUA", "HRA",
                           "IEA", "ITA", "LVA", "LTA", "LUA", "MTA", "NLA", "PLA", "PTA", "ROA", "SKA", "SIA", "ESA", "SEA")

  //TODO Is this all of them?
  val nonUkDisclosurePrefixes = List("ATD", "BED", "BGD", "CYD", "CZD", "DKD", "EED", "FID", "FRD", "DED", "GRD", "HUD", "HRD",
    "IED", "ITD", "LVD", "LTD", "LUD", "MTD", "NLD", "PLD", "PTD", "ROD", "SKD", "SID", "ESD", "SED")

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
      case Some(validArrangementId) if nonUkPrefixes.contains(validArrangementId.prefix) =>  Future(Some(true))
      case Some(validArrangementId) if validArrangementId.prefix.equals("GBA") => arrangementIdRepository.doesArrangementIdExist(validArrangementId).map(
                                       result => Some(result))
      case _ => Future(None)
    }

  def verifyDisclosureId(suppliedDisclosureId: String, enrolmentId: String): Future[Option[Boolean]] =

    createDisclosureIdFromSuppliedString(suppliedDisclosureId) match {
      case Some(validDisclosureId) if nonUkDisclosurePrefixes.contains(validDisclosureId.prefix) =>  Future(Some(true))
      case Some(validDisclosureId) if validDisclosureId.prefix.equals("GBD") =>
        submissionDetailsRepository.doesDisclosureIdMatchEnrolmentID(validDisclosureId.value, enrolmentId).map(
          result => Some(result))
      case _ => Future(None)
    }

  def createArrangementIdFromSuppliedString(suppliedString: String): Option[ArrangementId] = {

    if(suppliedString.matches(arrangementIdRegEx)) {
    Some(ArrangementId(prefix = suppliedString.substring(0,3),
                       dateString = suppliedString.substring(3, 11),
                       suffix = suppliedString.substring(11)))
    } else None

  }

  def createDisclosureIdFromSuppliedString(suppliedString: String): Option[DisclosureId] = {

    if(suppliedString.matches(disclosureIdRegEx)) {
      Some(DisclosureId(prefix = suppliedString.substring(0,3),
        dateString = suppliedString.substring(3, 11),
        suffix = suppliedString.substring(11)))
    } else None

  }
}
