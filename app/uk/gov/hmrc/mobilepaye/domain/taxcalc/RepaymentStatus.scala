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

import enumeratum.EnumEntry.UpperSnakecase
import enumeratum._

import scala.collection.immutable

sealed trait RepaymentStatus extends EnumEntry with UpperSnakecase

object RepaymentStatus extends Enum[RepaymentStatus] with PlayUppercaseJsonEnum[RepaymentStatus] {
  case object Refund            extends RepaymentStatus
  case object PaymentProcessing extends RepaymentStatus
  case object PaymentPaid       extends RepaymentStatus
  case object ChequeSent        extends RepaymentStatus
  case object SaUser            extends RepaymentStatus
  case object UnableToClaim     extends RepaymentStatus
  case object PaymentDue        extends RepaymentStatus
  case object PaidAll           extends RepaymentStatus
  case object PaidPart          extends RepaymentStatus
  case object PaymentsDown      extends RepaymentStatus

  override def values: immutable.IndexedSeq[RepaymentStatus] = findValues
}
