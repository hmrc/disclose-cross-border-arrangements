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

package services

import javax.inject.Inject
import models.GeneratedIDs

import scala.xml.{Elem, Node, NodeSeq}
import scala.xml.transform.{RewriteRule, RuleTransformer}

class TransformService @Inject()() {
  def transformFileForIDs(submissionFile: NodeSeq, ids: GeneratedIDs): NodeSeq =
    ids match {
      case GeneratedIDs(None, None) => submissionFile
      case GeneratedIDs(arrangementID, disclosureID) => {
          val arrangementChild = arrangementID.map(id => <ArrangementID>{id.value}</ArrangementID>)
          val disclosureChild = disclosureID.map(id => <DisclosureID>{id.value}</DisclosureID>)

          new RuleTransformer(new RewriteRule {
              override def transform(n: Node): Seq[Node] = n match {
                case elem : Elem if elem.label == "DAC6_Arrangement" && arrangementChild.isDefined =>
                  elem.copy(child = elem.child.find(_.label == "Header").get ++ arrangementChild.get ++ elem.child.find(_.label == "DAC6Disclosures").getOrElse(Seq.empty))
                case elem : Elem if elem.label == "DAC6Disclosures" && disclosureChild.isDefined =>
                  elem.copy(child = disclosureChild.get ++ elem.child)
                case other => other
              }
          }).transform(submissionFile).head
      }
    }

}
