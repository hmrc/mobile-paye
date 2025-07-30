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

package uk.gov.hmrc.mobilepaye.domain.taxcalc

import play.api.libs.json.{Format, JsResult, JsString, JsValue}

sealed trait P800Status

object P800Status {
  case object Underpaid    extends P800Status
  case object Overpaid     extends P800Status
  case object NotSupported extends P800Status

  implicit val format: Format[P800Status] = new Format[P800Status] {

    override def writes(o: P800Status): JsValue = o match {
      case Underpaid    => JsString("underpaid")
      case Overpaid     => JsString("overpaid")
      case NotSupported => JsString("not_supported")
    }

    override def reads(json: JsValue): JsResult[P800Status] =
      json.validate[String].map {
        case "underpaid" => Underpaid
        case "overpaid"  => Overpaid
        case _           => NotSupported
      }
  }
}
