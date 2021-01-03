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

import base.SpecBase

import scala.io.Source

class XmlValidationServiceSpec extends SpecBase {
  "Validation Service" - {
    "must pass back errors if a file is invalid" in {
      val service = app.injector.instanceOf[XMLValidationService]

      val invalid = <this>
      <will>not validate</will>
      </this>

      val result = service.validateXml(invalid.mkString)

      result.isLeft mustBe true
    }

    "must correctly invalidate a submission with a data problem" in {
      val service = app.injector.instanceOf[XMLValidationService]
      val validsubmission =
        Source
          .fromInputStream(getClass.getResourceAsStream("/invalid.xml"))
          .getLines.mkString("\n")

      val result = service.validateXml(validsubmission)

      result.isLeft mustBe true
    }

    "must correctly validate a submission" in {
      val service = app.injector.instanceOf[XMLValidationService]
      val validsubmission =
        Source
          .fromInputStream(getClass.getResourceAsStream("/valid.xml"))
          .getLines.mkString("\n")

      val result = service.validateXml(validsubmission)

      result.isLeft mustBe false
    }
  }
}
