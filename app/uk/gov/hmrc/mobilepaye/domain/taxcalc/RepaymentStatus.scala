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

import enumeratum._
import scala.collection.immutable

sealed trait RepaymentStatus extends EnumEntry

object RepaymentStatus extends Enum[RepaymentStatus] with PlayUppercaseJsonEnum[RepaymentStatus] {
  case object `REFUND`             extends RepaymentStatus
  case object `PAYMENT_PROCESSING` extends RepaymentStatus
  case object `PAYMENT_PAID`       extends RepaymentStatus
  case object `CHEQUE_SENT`        extends RepaymentStatus
  case object `SA_USER`            extends RepaymentStatus
  case object `UNABLE_TO_CLAIM`    extends RepaymentStatus
  case object `PAYMENT_DUE`        extends RepaymentStatus
  case object `PAID_ALL`           extends RepaymentStatus
  case object `PAID_PART`          extends RepaymentStatus
  case object `PAYMENTS_DOWN`      extends RepaymentStatus

  override def values: immutable.IndexedSeq[RepaymentStatus] = findValues
}