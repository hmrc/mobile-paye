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
  case object Refund extends RepaymentStatus
  case object PaymentProcessing extends RepaymentStatus
  case object PaymentPaid extends RepaymentStatus
  case object ChequeSent extends RepaymentStatus
  case object SaUser extends RepaymentStatus
  case object UnableToClaim extends RepaymentStatus
  case object PaymentDue extends RepaymentStatus
  case object PartPaid extends RepaymentStatus
  case object PaidAll extends RepaymentStatus
  case object PaymentsDown extends RepaymentStatus
  case object Unknown extends RepaymentStatus

  implicit val format: Format[RepaymentStatus] = new Format[RepaymentStatus] {
    override def reads(json: JsValue): JsResult[RepaymentStatus] =
      json.validate[String].map {
        case "refund"             => Refund
        case "payment_processing" => PaymentProcessing
        case "payment_paid"       => PaymentPaid
        case "cheque_sent"        => ChequeSent
        case "sa_user"            => SaUser
        case "unable_to_claim"    => UnableToClaim
        case "payment_due"        => PaymentDue
        case "part_paid"          => PartPaid
        case "paid_all"           => PaidAll
        case "payments_down"      => PaymentsDown
        case "unknown"            => Unknown
      }

    override def writes(o: RepaymentStatus): JsValue =
      o match {
        case Refund            => JsString("refund")
        case PaymentProcessing => JsString("payment_processing")
        case PaymentPaid       => JsString("payment_paid")
        case ChequeSent        => JsString("cheque_sent")
        case SaUser            => JsString("sa_user")
        case UnableToClaim     => JsString("unable_to_claim")
      }
  }

}
