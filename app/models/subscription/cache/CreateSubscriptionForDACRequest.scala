/*
 * Copyright 2023 HM Revenue & Customs
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

package models.subscription.cache

import controllers.APIDateTimeFormats.localDateTimeWrites
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

import java.time.LocalDateTime

case class CreateSubscriptionForDACRequest(
  createSubscriptionForDACRequest: SubscriptionForDACRequest,
  subscriptionID: String,
  lastUpdated: LocalDateTime = LocalDateTime.now
)

object CreateSubscriptionForDACRequest {

  val format: OFormat[CreateSubscriptionForDACRequest] = OFormat(reads, writes)

  implicit lazy val reads: Reads[CreateSubscriptionForDACRequest] = {
    import play.api.libs.functional.syntax._
    (
      (__ \\ "createSubscriptionForDACRequest").read[SubscriptionForDACRequest] and
        (__ \\ "subscriptionID").read[String] and
        (__ \\ "lastUpdated").readNullable[LocalDateTime]
    )(
      (subscription, subscriptionID, lastUpdated) => CreateSubscriptionForDACRequest(subscription, subscriptionID, lastUpdated.getOrElse(LocalDateTime.now))
    )
  }

  implicit lazy val writes: OWrites[CreateSubscriptionForDACRequest] = (
    (__ \ "createSubscriptionForDACRequest").write[SubscriptionForDACRequest] and
      (__ \ "subscriptionID").write[String] and
      (__ \ "lastUpdated").write[LocalDateTime](localDateTimeWrites)
  )(
    r => (r.createSubscriptionForDACRequest, r.subscriptionID, r.lastUpdated)
  )
}

case class RequestCommonForSubscription(regime: String,
                                        receiptDate: String,
                                        acknowledgementReference: String,
                                        originatingSystem: String,
                                        requestParameters: Option[Seq[RequestParameter]]
)

object RequestCommonForSubscription {
  implicit val format = Json.format[RequestCommonForSubscription]
}

case class OrganisationDetails(organisationName: String)

object OrganisationDetails {
  implicit val format: OFormat[OrganisationDetails] = Json.format[OrganisationDetails]
}

case class IndividualDetails(firstName: String, middleName: Option[String], lastName: String)

object IndividualDetails {
  implicit val format: OFormat[IndividualDetails] = Json.format[IndividualDetails]
}

sealed trait ContactInformation

case class ContactInformationForIndividual(individual: IndividualDetails, email: String, phone: Option[String], mobile: Option[String])
    extends ContactInformation

object ContactInformationForIndividual {
  implicit val format: OFormat[ContactInformationForIndividual] = Json.format[ContactInformationForIndividual]
}

case class ContactInformationForOrganisation(organisation: OrganisationDetails, email: String, phone: Option[String], mobile: Option[String])
    extends ContactInformation

object ContactInformationForOrganisation {
  implicit val format: OFormat[ContactInformationForOrganisation] = Json.format[ContactInformationForOrganisation]
}

case class PrimaryContact(contactInformation: ContactInformation)

object PrimaryContact {

  implicit lazy val reads: Reads[PrimaryContact] = {
    import play.api.libs.functional.syntax._
    (
      (__ \\ "organisation").readNullable[OrganisationDetails] and
        (__ \\ "individual").readNullable[IndividualDetails] and
        (__ \\ "email").read[String] and
        (__ \\ "phone").readNullable[String] and
        (__ \\ "mobile").readNullable[String]
    )(
      (organisation, individual, email, phone, mobile) =>
        (organisation, individual) match {
          case (Some(_), Some(_)) => throw new Exception("PrimaryContact cannot have both and organisation or individual element")
          case (Some(org), _)     => PrimaryContact(ContactInformationForOrganisation(org, email, phone, mobile))
          case (_, Some(ind))     => PrimaryContact(ContactInformationForIndividual(ind, email, phone, mobile))
          case (None, None)       => throw new Exception("PrimaryContact must have either an organisation or individual element")
        }
    )
  }

  implicit lazy val writes: OWrites[PrimaryContact] = {
    case PrimaryContact(contactInformationForInd @ ContactInformationForIndividual(_, _, _, _)) =>
      Json.toJsObject(contactInformationForInd)
    case PrimaryContact(contactInformationForOrg @ ContactInformationForOrganisation(_, _, _, _)) =>
      Json.toJsObject(contactInformationForOrg)
  }
}

case class SecondaryContact(contactInformation: ContactInformation)

object SecondaryContact {

  implicit lazy val reads: Reads[SecondaryContact] = {
    import play.api.libs.functional.syntax._
    (
      (__ \\ "organisation").readNullable[OrganisationDetails] and
        (__ \\ "individual").readNullable[IndividualDetails] and
        (__ \\ "email").read[String] and
        (__ \\ "phone").readNullable[String] and
        (__ \\ "mobile").readNullable[String]
    )(
      (organisation, individual, email, phone, mobile) =>
        (organisation, individual) match {
          case (Some(_), Some(_)) => throw new Exception("SecondaryContact cannot have both and organisation or individual element")
          case (Some(org), _)     => SecondaryContact(ContactInformationForOrganisation(org, email, phone, mobile))
          case (_, Some(ind))     => SecondaryContact(ContactInformationForIndividual(ind, email, phone, mobile))
          case (None, None)       => throw new Exception("SecondaryContact must have either an organisation or individual element")
        }
    )
  }

  implicit lazy val writes: OWrites[SecondaryContact] = {
    case SecondaryContact(contactInformationForInd @ ContactInformationForIndividual(_, _, _, _)) =>
      Json.toJsObject(contactInformationForInd)
    case SecondaryContact(contactInformationForOrg @ ContactInformationForOrganisation(_, _, _, _)) =>
      Json.toJsObject(contactInformationForOrg)
  }
}

case class RequestDetail(IDType: String,
                         IDNumber: String,
                         tradingName: Option[String],
                         isGBUser: Boolean,
                         primaryContact: PrimaryContact,
                         secondaryContact: Option[SecondaryContact]
)

object RequestDetail {

  implicit val reads: Reads[RequestDetail] = {
    import play.api.libs.functional.syntax._
    (
      (__ \ "IDType").read[String] and
        (__ \ "IDNumber").read[String] and
        (__ \ "tradingName").readNullable[String] and
        (__ \ "isGBUser").read[Boolean] and
        (__ \ "primaryContact").read[PrimaryContact] and
        (__ \ "secondaryContact").readNullable[SecondaryContact]
    )(
      (idType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact) =>
        RequestDetail(idType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
    )
  }

  implicit val writes: OWrites[RequestDetail] = Json.writes[RequestDetail]
}

case class SubscriptionForDACRequest(requestCommon: RequestCommonForSubscription, requestDetail: RequestDetail)

object SubscriptionForDACRequest {
  implicit val format: OFormat[SubscriptionForDACRequest] = Json.format[SubscriptionForDACRequest]
}

case class RequestParameter(paramName: String, paramValue: String)

object RequestParameter {
  implicit val format: OFormat[RequestParameter] = Json.format[RequestParameter]
}
