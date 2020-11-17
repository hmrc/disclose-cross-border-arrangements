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

import models.subscription._
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}

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

  def updateDetailsPayloadNoSecondContact(idNumber: JsString,
                                          isGBUser: JsBoolean,
                                          firstName: JsString,
                                          lastName: JsString,
                                          primaryEmail: JsString): String = {
    s"""
       |{
       |  "updateSubscriptionForDACRequest": {
       |    "requestCommon": {
       |      "regime": "DAC",
       |      "receiptDate": "2020-09-23T16:12:11Z",
       |      "acknowledgementReference": "AB123c",
       |      "originatingSystem": "MDTP",
       |      "requestParameters": [{
       |        "paramName":"Name",
       |        "paramValue":"Value"
       |      }]
       |    },
       |    "requestDetail": {
       |      "IDType": "SAFE",
       |      "IDNumber": $idNumber,
       |      "isGBUser": $isGBUser,
       |      "primaryContact": [{
       |        "individual": {
       |          "firstName": $firstName,
       |          "lastName": $lastName
       |        },
       |        "email": $primaryEmail
       |      }]
       |    }
       |  }
       |}
       |""".stripMargin
  }

  def updateDetailsPayload(idNumber: JsString,
                           isGBUser: JsBoolean,
                           firstName: JsString,
                           lastName: JsString,
                           email: JsString,
                           organisationName: JsString,
                           phone: JsString): String = {
    s"""
       |{
       |  "updateSubscriptionForDACRequest": {
       |    "requestCommon": {
       |      "regime": "DAC",
       |      "receiptDate": "2020-09-23T16:12:11Z",
       |      "acknowledgementReference": "AB123c",
       |      "originatingSystem": "MDTP",
       |      "requestParameters": [{
       |        "paramName":"Name",
       |        "paramValue":"Value"
       |      }]
       |    },
       |    "requestDetail": {
       |      "IDType": "SAFE",
       |      "IDNumber": $idNumber,
       |      "isGBUser": $isGBUser,
       |      "primaryContact": [{
       |        "individual": {
       |          "firstName": $firstName,
       |          "lastName": $lastName
       |        },
       |        "email": $email
       |      }],
       |      "secondaryContact": [{
       |        "organisation": {
       |          "organisationName": $organisationName
       |        },
       |        "email": $email,
       |        "phone": $phone
       |      }]
       |    }
       |  }
       |}
       |""".stripMargin
  }

  def updateDetailsJsonNoSecondContact(idNumber: String,
                                       isGBUser: Boolean,
                                       firstName: String,
                                       lastName: String,
                                       primaryEmail: String): JsObject = {
    Json.obj(
      "updateSubscriptionForDACRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "regime" -> "DAC",
          "receiptDate" -> "2020-09-23T16:12:11Z",
          "acknowledgementReference" -> "AB123c",
          "originatingSystem" -> "MDTP",
          "requestParameters" -> Json.arr(
            Json.obj(
              "paramName" -> "Name",
              "paramValue" -> "Value"
            )
          )
        ),
        "requestDetail" -> Json.obj(
          "IDType" -> "SAFE",
          "IDNumber" -> idNumber,
          "isGBUser" -> isGBUser,
          "primaryContact" -> Json.obj(
            "individual" -> Json.obj(
              "firstName" -> firstName,
              "lastName" -> lastName
            ),
            "email" -> primaryEmail
          )

        )
      )
    )
  }

  def updateDetailsJson(idNumber: String,
                        isGBUser: Boolean,
                        firstName: String,
                        lastName: String,
                        email: String,
                        organisationName: String,
                        phone: String): JsObject = {
    Json.obj(
      "updateSubscriptionForDACRequest" -> Json.obj(
        "requestCommon" -> Json.obj(
          "regime" -> "DAC",
          "receiptDate" -> "2020-09-23T16:12:11Z",
          "acknowledgementReference" -> "AB123c",
          "originatingSystem" -> "MDTP",
          "requestParameters" -> Json.arr(
            Json.obj(
              "paramName" -> "Name",
              "paramValue" -> "Value"
            )
          )
        ),
        "requestDetail" -> Json.obj(
          "IDType" -> "SAFE",
          "IDNumber" -> idNumber,
          "isGBUser" -> isGBUser,
          "primaryContact" -> Json.obj(
            "individual" -> Json.obj(
              "firstName" -> firstName,
              "lastName" -> lastName
            ),
            "email" -> email
          ),
          "secondaryContact" -> Json.obj(
            "organisation" -> Json.obj(
              "organisationName" -> organisationName
            ),
            "email" -> email,
            "phone" -> phone
          )

        )
      )
    )
  }

}
