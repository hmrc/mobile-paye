/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.domain

import play.api.libs.json.{Json, OFormat}

case class MobilePayeResponseAudit(
  taxYear:            Option[Int],
  employments:        Option[Seq[PayeIncomeAudit]],
  pensions:           Option[Seq[PayeIncomeAudit]],
  repayment:          Option[P800Repayment],
  otherIncomes:       Option[Seq[OtherIncomeAudit]],
  taxFreeAmount:      Option[BigDecimal],
  estimatedTaxAmount: Option[BigDecimal])

object MobilePayeResponseAudit {

  def fromResponse(response: MobilePayeResponse): MobilePayeResponseAudit =
    MobilePayeResponseAudit(
      taxYear            = response.taxYear,
      employments        = response.employments.map(emps => emps.map(PayeIncomeAudit.fromPayeIncome)),
      pensions           = response.pensions.map(pens => pens.map(PayeIncomeAudit.fromPayeIncome)),
      repayment          = response.repayment,
      otherIncomes       = response.otherIncomes.map(oIncs => oIncs.map(OtherIncomeAudit.fromOtherIncome)),
      taxFreeAmount      = response.taxFreeAmount,
      estimatedTaxAmount = response.estimatedTaxAmount
    )

  implicit val format: OFormat[MobilePayeResponseAudit] = Json.format[MobilePayeResponseAudit]
}
