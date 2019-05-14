/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json._

sealed trait RepaymentStatus

object RepaymentStatus {
  case object Refund            extends RepaymentStatus
  case object PaymentProcessing extends RepaymentStatus
  case object PaymentPaid       extends RepaymentStatus
  case object ChequeSent        extends RepaymentStatus
  case object SaUser            extends RepaymentStatus
  case object UnableToClaim     extends RepaymentStatus

  implicit val format: Format[RepaymentStatus] = new Format[RepaymentStatus] {
    override def reads(json: JsValue): JsResult[RepaymentStatus] = {
      json.validate[String].map {
        case "REFUND"             => Refund
        case "PAYMENT_PROCESSING" => PaymentProcessing
        case "PAYMENT_PAID"       => PaymentPaid
        case "CHEQUE_SENT"        => ChequeSent
        case "SA_USER"            => SaUser
        case "UNABLE_TO_CLAIM"    => UnableToClaim
      }
    }

    override def writes(o: RepaymentStatus): JsValue = {
      o match {
        case Refund            => JsString("REFUND")
        case PaymentProcessing => JsString("PAYMENT_PROCESSING")
        case PaymentPaid       => JsString("PAYMENT_PAID")
        case ChequeSent        => JsString("CHEQUE_SENT")
        case SaUser            => JsString("SA_USER")
        case UnableToClaim     => JsString("UNABLE_TO_CLAIM")
      }
    }
  }
}