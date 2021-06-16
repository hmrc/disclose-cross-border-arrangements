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

import base.SpecBase
import models.DisclosureId

class DisclosureIdRepositorySpec extends SpecBase {
  "Disclosure Id Repository" - {

    "must store disclosure Id correctly" in {

        val repo = app.injector.instanceOf[DisclosureIdRepository]

      implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
        val disclosureId = DisclosureId(dateString = "date", suffix = "suffix")
        whenReady (repo.storeDisclosureId(disclosureId).flatMap(_ => repo.doesDisclosureIdExist(disclosureId))) { result =>
          result mustBe true
        }
      }
  }
}
