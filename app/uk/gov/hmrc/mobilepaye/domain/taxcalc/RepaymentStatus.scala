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
      json.as[String].toLowerCase() match {
        case "refund"             => JsSuccess(Refund)
        case "payment_processing" => JsSuccess(PaymentProcessing)
        case "payment_paid"       => JsSuccess(PaymentPaid)
        case "cheque_sent"        => JsSuccess(ChequeSent)
        case "sa_user"            => JsSuccess(SaUser)
        case "unable_to_claim"    => JsSuccess(UnableToClaim)
        case "payment_due"        => JsSuccess(PaymentDue)
        case "part_paid"          => JsSuccess(PartPaid)
        case "paid_all"           => JsSuccess(PaidAll)
        case "payments_down"      => JsSuccess(PaymentsDown)
        case "unknown"            => JsSuccess(Unknown)
        case _                    => JsError("unknown Repayment Status")
      }

    override def writes(o: RepaymentStatus): JsValue =
      o match {
        case Refund            => JsString("REFUND")
        case PaymentProcessing => JsString("PAYMENT_PROCESSING")
        case PaymentPaid       => JsString("PAYMENT_PAID")
        case ChequeSent        => JsString("CHEQUE_SENT")
        case SaUser            => JsString("SA_USER")
        case UnableToClaim     => JsString("UNABLE_TO_CLAIM")
        case PaymentDue        => JsString("PAYMENT_DUE")
        case PartPaid          => JsString("PART_PAID")
        case PaidAll           => JsString("PAID_ALL")
        case PaymentsDown      => JsString("PAYMENTS_DOWN")
        case Unknown           => JsString("UNKNOWN")
      }
  }

}
