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

package uk.gov.hmrc.mobilepaye.domain.audit

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mobilepaye.domain._
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.{MobileSimpleAssessmentResponse, TempMobileSimpleAssessmentResponse}

case class MobilePayeSummaryResponseAudit(
  taxYear:             Option[Int],
  employments:         Option[Seq[PayeIncomeAudit]],
  previousEmployments: Option[Seq[PayeIncomeAudit]],
  pensions:            Option[Seq[PayeIncomeAudit]],
  repayment:           Option[P800Repayment],
  otherIncomes:        Option[Seq[OtherIncomeAudit]],
  taxCodeChange:       Option[TaxCodeChange],
  simpleAssessment:    Option[TempMobileSimpleAssessmentResponse],
  taxFreeAmount:       Option[BigDecimal],
  estimatedTaxAmount:  Option[BigDecimal])

object MobilePayeSummaryResponseAudit {

  def fromResponse(response: TempMobilePayeSummaryResponse): MobilePayeSummaryResponseAudit =
    MobilePayeSummaryResponseAudit(
      taxYear             = response.taxYear,
      employments         = response.employments.map(emps => emps.map(PayeIncomeAudit.fromPayeIncome)),
      previousEmployments = response.previousEmployments.map(emps => emps.map(PayeIncomeAudit.fromPayeIncome)),
      pensions            = response.pensions.map(pens => pens.map(PayeIncomeAudit.fromPayeIncome)),
      repayment           = response.repayment,
      otherIncomes        = response.otherIncomes.map(oIncs => oIncs.map(OtherIncomeAudit.fromOtherIncome)),
      taxCodeChange       = response.taxCodeChange,
      simpleAssessment    = response.simpleAssessment,
      taxFreeAmount       = response.taxFreeAmount,
      estimatedTaxAmount  = response.estimatedTaxAmount
    )

  def fromPYResponse(response: MobilePayePreviousYearSummaryResponse): MobilePayeSummaryResponseAudit =
    MobilePayeSummaryResponseAudit(
      taxYear             = response.taxYear,
      employments         = response.employments.map(emps => emps.map(PayeIncomeAudit.fromPayeIncome)),
      previousEmployments = response.previousEmployments.map(emps => emps.map(PayeIncomeAudit.fromPayeIncome)),
      pensions            = response.pensions.map(pens => pens.map(PayeIncomeAudit.fromPayeIncome)),
      repayment           = None,
      otherIncomes        = response.otherIncomes.map(oIncs => oIncs.map(OtherIncomeAudit.fromOtherIncome)),
      taxCodeChange       = None,
      simpleAssessment    = None,
      taxFreeAmount       = response.taxFreeAmount,
      estimatedTaxAmount  = response.estimatedTaxAmount
    )

  implicit val format: OFormat[MobilePayeSummaryResponseAudit] = Json.format[MobilePayeSummaryResponseAudit]
}
