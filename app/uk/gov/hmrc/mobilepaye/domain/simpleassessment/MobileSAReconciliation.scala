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

package uk.gov.hmrc.mobilepaye.domain.simpleassessment

import play.api.libs.json.{Format, Json, OFormat, Reads}
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.ReasonType.ReasonType
import uk.gov.hmrc.mobilepaye.utils.EnumUtils

case class MobileSAReconciliation(
  reconciliationId:         Int,
  reconciliationStatus:     Option[String] = None,
  cumulativeAmount:         Double,
  taxLiabilityAmount:       Double,
  taxPaidAmount:            Double,
  reconciliationTimeStamp:  Option[String] = None,
  p800Status:               Option[String],
  collectionMethod:         Option[Int] = None,
  previousReconciliationId: Option[Int] = None,
  nextReconciliationId:     Option[Int] = None,
  multiYearRecIndicator:    Option[Boolean] = None,
  p800Reasons:              Option[List[Reason]] = None,
  businessReason:           String,
  eligibility:              Boolean,
  totalAmountOwed:          Double,
  chargeReference:          Option[String] = None,
  dueDate:                  Option[String] = None,
  dunningLock:              Option[Boolean] = None,
  receivableStatus:         String,
  receipts:                 Option[List[Receipts]] = None)

object MobileSAReconciliation {

  implicit val formats: Format[MobileSAReconciliation] = Json.format[MobileSAReconciliation]
}

case class Reason(
  reasonType:      ReasonType,
  reasonCode:      Int,
  estimatedAmount: Option[Double] = None,
  actualAmount:    Option[Double] = None)

object Reason {
  implicit val formats: Format[Reason] = Json.format[Reason]
}

object ReasonType extends Enumeration {
  type ReasonType = Value
  val UNDERPAYMENT:             ReasonType        = Value(9)
  val OVERPAYMENT:              ReasonType        = Value(10)
  implicit val readsReasonType: Reads[ReasonType] = EnumUtils.enumReads(ReasonType)
}

case class Receipts(
  receiptAmount:      Option[Double],
  receiptDate:        Option[String],
  receiptMethod:      Option[String] = None,
  receiptStatus:      Option[String] = None,
  taxYearCodedOut:    Option[String] = None,
  allocatedAmount:    Option[Double] = None,
  promiseToPayRef:    Option[String] = None,
  receiptDescription: Option[String] = None)

object Receipts {
  implicit val formats: OFormat[Receipts] = Json.format[Receipts]
}
