/*
 * Copyright 2020 HM Revenue & Customs
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
import uk.gov.hmrc.time.TaxYear

case class MobilePayeResponse(
  taxYear:                   Option[Int],
  employments:               Option[Seq[PayeIncome]],
  pensions:                  Option[Seq[PayeIncome]],
  repayment:                 Option[P800Repayment],
  otherIncomes:              Option[Seq[OtherIncome]],
  taxFreeAmount:             Option[BigDecimal],
  taxFreeAmountLink:         Option[String] = Some("/check-income-tax/tax-free-allowance"),
  estimatedTaxAmount:        Option[BigDecimal],
  estimatedTaxAmountLink:    Option[String] = Some("/check-income-tax/paye-income-tax-estimate"),
  understandYourTaxCodeLink: Option[String] = Some("/check-income-tax/tax-codes"),
  addMissingEmployerLink:    Option[String] = Some("/check-income-tax/add-employment/employment-name"),
  addMissingPensionLink:     Option[String] = Some("/check-income-tax/add-pension-provider/name"),
  addMissingIncomeLink:      Option[String] = Some("/forms/form/tell-us-about-other-income/guide"),
  previousTaxYearLink:       Option[String] = Some(s"/check-income-tax/historic-paye/${TaxYear.current.previous.startYear}"))

object MobilePayeResponse {

  def empty: MobilePayeResponse =
    MobilePayeResponse(
      taxYear                   = Option(TaxYear.current.currentYear),
      employments               = None,
      pensions                  = None,
      repayment                 = None,
      otherIncomes              = None,
      taxFreeAmount             = None,
      taxFreeAmountLink         = None,
      estimatedTaxAmount        = None,
      estimatedTaxAmountLink    = None,
      understandYourTaxCodeLink = None
    )

  implicit val format: OFormat[MobilePayeResponse] = Json.format[MobilePayeResponse]
}
