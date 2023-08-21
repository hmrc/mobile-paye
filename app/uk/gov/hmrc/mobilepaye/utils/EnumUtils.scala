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

package uk.gov.hmrc.mobilepaye.utils

import play.api.Logging
import play.api.libs.json._

object EnumUtils extends Logging {

  def enumReads[E <: Enumeration](`enum`: E): Reads[E#Value] =
    new Reads[E#Value] {

      def reads(json: JsValue): JsResult[E#Value] = json match {
        case JsString(s) =>
          try JsSuccess(enum.withName(s))
          catch {
            case e: NoSuchElementException =>
              logger.error(s"[Enumeration] Validation failure: $s  is not a valid ${enum.toString}")
              throw e
          }
        case JsNumber(n) =>
          JsSuccess(enum(n.toInt))
        case _ => JsError("String value expected")
      }
    }
}
