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

package models

import base.SpecBase

class GenericErrorSpec extends SpecBase {

  val mockGenericErrorSeq = Seq(GenericError(4, "British Shorthair"), GenericError(1, "Maine Coon"),
    GenericError(3, "Siamese Cat"), GenericError(2, "Ragdoll"))

  "orderByLineNumber" - {

    "must return a sequence of Generic Errors ordered by lineNumber" in {

      mockGenericErrorSeq.sorted mustBe Seq(GenericError(1, "Maine Coon"), GenericError(2, "Ragdoll"), GenericError(3, "Siamese Cat"), GenericError(4, "British Shorthair"))

    }
  }
}