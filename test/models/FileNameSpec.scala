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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import org.scalatest.{FreeSpec, MustMatchers}

class FileNameSpec extends FreeSpec with MustMatchers {

  object File extends FileName("fileName", "disclosureID", genId, time)

  val genId: GeneratedIDs = GeneratedIDs(Some(ArrangementId("GBA", "20200601", "AAA000")), Some(DisclosureId("GBD", "20200701", "AAA001")))
  val time: LocalDateTime = LocalDateTime.now
  val format: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

  ".toString" - {

    "must return the correct string" in {

      File.toString mustEqual "fileName-GBD20200701AAA001-" + time.format(format)
    }
  }
}

