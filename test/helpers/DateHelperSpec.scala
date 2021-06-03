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

package helpers

import java.time.{LocalDate, LocalDateTime}

import base.SpecBase

class DateHelperSpec extends SpecBase {

  val helper = new DateHelper

  "DateHelper" - {
    "should generate the time" in {

      val date = helper.today

      date mustBe LocalDate.now()

    }

    "should generate the date time" in {

      val dateTime = helper.now

      dateTime mustBe LocalDateTime.now()

    }
  }
}