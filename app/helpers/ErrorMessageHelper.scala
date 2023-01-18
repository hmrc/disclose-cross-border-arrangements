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

package helpers

import scala.xml.PrettyPrinter

class ErrorMessageHelper {

  import models.{GenericError, Validation}

  val WIDTH = 1000000
  val STEP  = 2

  import scala.xml.Elem

  def convertToGenericErrors(validations: Seq[Validation], xml: Elem): Seq[GenericError] = {

    val prettyPrinter = new PrettyPrinter(WIDTH, STEP)

    val xmlArray = prettyPrinter.formatNodes(xml).split("\n")

    val validationWithLineNumber = validations.map(
      validation => validation.setLineNumber(xmlArray)
    )

    validationWithLineNumber.map(
      validation => validation.toGenericError
    )

  }
}
