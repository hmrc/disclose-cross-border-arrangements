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

package services

import models.subscription.cache.{CreateSubscriptionForDACRequest, IndividualDetails, OrganisationDetails}
import models.subscription.{
  ContactInformation,
  ContactInformationForIndividual,
  ContactInformationForOrganisation,
  DisplaySubscriptionForDACResponse,
  PrimaryContact,
  ResponseCommon,
  ResponseDetail,
  SecondaryContact,
  SubscriptionForDACResponse
}
import repositories.SubscriptionCacheRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionCacheService @Inject() (cacheRepository: SubscriptionCacheRepository) {

  def storeSubscriptionDetails(id: String, subscription: CreateSubscriptionForDACRequest): Future[Boolean] =
    cacheRepository.set(id, subscription)

  def retrieveSubscriptionDetails(id: String)(implicit ec: ExecutionContext): Future[Option[DisplaySubscriptionForDACResponse]] = {
    //Fake response message from our cached details
    cacheRepository.get(id).map {
      result =>
        result.map {
          subRequest =>
            val requestDetail = subRequest.createSubscriptionForDACRequest.requestDetail
            DisplaySubscriptionForDACResponse(
              SubscriptionForDACResponse(
                ResponseCommon("", None, "", None),
                ResponseDetail(
                  subRequest.subscriptionID,
                  requestDetail.tradingName,
                  requestDetail.isGBUser,
                  PrimaryContact(Seq(convertContactInformation(requestDetail.primaryContact.contactInformation))),
                  requestDetail.secondaryContact.map(
                    a => SecondaryContact(Seq(convertContactInformation(a.contactInformation)))
                  )
                )
              )
            )
        }
    }
  }

  private def convertContactInformation(contactInformation: models.subscription.cache.ContactInformation): ContactInformation =
    contactInformation match {
      case models.subscription.cache.ContactInformationForIndividual(individual, email, phone, mobile) =>
        ContactInformationForIndividual(convertIndividual(individual), email, phone, mobile)
      case models.subscription.cache.ContactInformationForOrganisation(organisation, email, phone, mobile) =>
        ContactInformationForOrganisation(convertOrganisation(organisation), email, phone, mobile)
    }

  private def convertIndividual(individualDetails: IndividualDetails): models.subscription.IndividualDetails =
    models.subscription.IndividualDetails(individualDetails.firstName, individualDetails.lastName, individualDetails.middleName)

  private def convertOrganisation(organisation: OrganisationDetails) =
    models.subscription.OrganisationDetails(organisation.organisationName)
}
