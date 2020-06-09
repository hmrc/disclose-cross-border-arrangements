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

import base.SpecBase
import uk.gov.hmrc.disclosecrossborderarrangements.helpers.SuffixHelper

class SuffixHelperSpec extends SpecBase {

  val helper = new SuffixHelper
  val arrangementIdSuffixRegex = "[A-Z0-9]{6}"


  "SuffixHelper" - {
    "Generate suffix" - {
      "should generate suffix which matches arrangementId regex" in {

        val suffix = helper.generateSuffix()

        suffix.matches(arrangementIdSuffixRegex) mustBe true

      }

    }
  }
}