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

package helpers

import models._

object JsonFixtures {

  val contactsModel =
    DisplaySubscriptionForDACResponse(
      SubscriptionForDACResponse(
        ResponseCommon("OK", None, "2020-08-09T11:23:45Z", None),
        ResponseDetail(
          "111111111",
          Some(""),
          true,
          PrimaryContact(Seq(
            ContactInformationForIndividual(IndividualDetails("First","Last", None), "", Some(""), Some("")
          ))),
          Some(SecondaryContact(Seq(
            ContactInformationForOrganisation(OrganisationDetails(""), "", None, None))
        ))
        )))

  val contactsResponse =
    """
      |{
      |"displaySubscriptionForDACResponse": {
      |"responseCommon": {
      |"status": "OK",
      |"processingDate": "2020-08-09T11:23:45Z"
      |},
      |"responseDetail": {
      |"subscriptionID": "111111111",
      |"tradingName": "",
      |"isGBUser": true,
      |"primaryContact": [
      |{
      |"email": "",
      |"phone": "",
      |"mobile": "",
      |"individual": {
      |"lastName": "Last",
      |"firstName": "First"
      |}
      |}
      |],
      |"secondaryContact": [
      |{
      |"email": "",
      |"organisation": {
      |"organisationName": ""
      |}
      |}
      |]
      |}
      |}
      |}""".stripMargin

}
