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

package uk.gov.hmrc.mobilepaye.domain

import ai.x.play.json.Encoders.encoder
import ai.x.play.json.Jsonx
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.MobileSimpleAssessmentResponse
import uk.gov.hmrc.time.TaxYear

case class MobilePayeSummaryResponse(
  taxYear:                   Option[Int],
  employments:               Option[Seq[PayeIncome]],
  previousEmployments:       Option[Seq[PayeIncome]],
  pensions:                  Option[Seq[PayeIncome]],
  repayment:                 Option[P800Repayment],
  otherIncomes:              Option[Seq[OtherIncome]],
  taxCodeChange:             Option[TaxCodeChange],
  simpleAssessment:          Option[MobileSimpleAssessmentResponse],
  taxFreeAmount:             Option[BigDecimal],
  taxFreeAmountLink:         Option[String] = Some("/check-income-tax/tax-free-allowance"),
  estimatedTaxAmount:        Option[BigDecimal],
  estimatedTaxAmountLink:    Option[String] = Some("/check-income-tax/paye-income-tax-estimate"),
  understandYourTaxCodeLink: Option[String] = Some("/check-income-tax/tax-codes"),
  addMissingEmployerLink:    String = "/check-income-tax/add-employment/employment-name",
  addMissingPensionLink:     String = "/check-income-tax/add-pension-provider/name",
  addMissingIncomeLink:      String = "/digital-forms/form/tell-us-about-other-income/draft/guide",
  addMissingBenefitLink:     String = "/digital-forms/form/tell-us-about-company-benefits/draft/guide",
  addMissingCompanyCarLink:  String = "/paye/company-car/do-you-pay-towards-car/",
  previousTaxYearLink:       String = "/check-income-tax/income-tax-history",
  updateEstimatedIncomeLink: String = "/check-income-tax/update-income/start",
  updateEmployerLink:        String = "/check-income-tax/update-remove-employment/decision-page",
  currentYearPlusOneLink:    Option[String] = Some("/check-income-tax/income-tax-comparison"),
  taxCodeLocation:           Option[String] = None,
  incomeTaxHistoricPayeUrl:  String = "/check-income-tax/historic-paye/")

object MobilePayeSummaryResponse {

  def empty: MobilePayeSummaryResponse =
    MobilePayeSummaryResponse(
      taxYear                   = Option(TaxYear.current.currentYear),
      employments               = None,
      previousEmployments       = None,
      pensions                  = None,
      repayment                 = None,
      otherIncomes              = None,
      simpleAssessment          = None,
      taxCodeChange             = None,
      taxFreeAmount             = None,
      taxFreeAmountLink         = None,
      estimatedTaxAmount        = None,
      estimatedTaxAmountLink    = None,
      understandYourTaxCodeLink = None,
      currentYearPlusOneLink    = None
    )

  implicit val format: OFormat[MobilePayeSummaryResponse] = Jsonx.formatCaseClass
}
