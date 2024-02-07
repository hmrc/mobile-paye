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

import play.api.libs.json.{Format, Json}
case class TempSandboxMobileSAReconciliation(
  reconciliationId:         Int,
  reconciliationStatus:     Option[Int] = None,
  cumulativeAmount:         Double,
  taxLiabilityAmount:       Double,
  taxPaidAmount:            Double,
  reconciliationTimeStamp:  Option[String] = None,
  p800Status:               Option[Int],
  collectionMethod:         Option[Int] = None,
  previousReconciliationId: Option[Int] = None,
  nextReconciliationId:     Option[Int] = None,
  multiYearRecIndicator:    Option[Boolean] = None,
  p800Reasons:              Option[List[TempSandboxReason]] = None,
  businessReason:           String,
  eligibility:              Boolean,
  totalAmountOwed:          Double,
  chargeReference:          Option[String] = None,
  dueDate:                  Option[String] = None,
  dunningLock:              Option[Boolean] = None,
  receivableStatus:         String,
  receipts:                 Option[List[Receipts]] = None)

object TempSandboxMobileSAReconciliation {

  implicit val formats: Format[TempSandboxMobileSAReconciliation] = Json.format[TempSandboxMobileSAReconciliation]
}

case class TempSandboxReason(
  reasonType:      Int,
  reasonCode:      Int,
  estimatedAmount: Option[Double] = None,
  actualAmount:    Option[Double] = None)

object TempSandboxReason {
  implicit val formats: Format[TempSandboxReason] = Json.format[TempSandboxReason]
}
