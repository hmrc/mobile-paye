/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.*
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.MobileSimpleAssessmentResponse
import uk.gov.hmrc.time.TaxYear

case class MobilePayeSummaryResponse(taxYear: Option[Int],
                                     employments: Option[Seq[PayeIncome]],
                                     previousEmployments: Option[Seq[PayeIncome]],
                                     pensions: Option[Seq[PayeIncome]],
                                     repayment: Option[P800Repayment],
                                     otherIncomes: Option[Seq[OtherIncome]],
                                     taxCodeChange: Option[TaxCodeChange],
                                     simpleAssessment: Option[MobileSimpleAssessmentResponse],
                                     taxFreeAmount: Option[BigDecimal],
                                     taxFreeAmountLink: Option[String] = Some("/check-income-tax/tax-free-allowance"),
                                     estimatedTaxAmount: Option[BigDecimal],
                                     estimatedTaxAmountLink: Option[String] = Some("/check-income-tax/paye-income-tax-estimate"),
                                     understandYourTaxCodeLink: Option[String] = Some("/check-income-tax/tax-codes"),
                                     addMissingEmployerLink: String = "/check-income-tax/add-employment/employment-name",
                                     addMissingPensionLink: String = "/check-income-tax/add-pension-provider/name",
                                     addMissingIncomeLink: String = "/digital-forms/form/tell-us-about-other-income/draft/guide",
                                     addMissingBenefitLink: String = "/submissions/new-form/tell-hmrc-about-your-company-benefits/",
                                     addMissingCompanyCarLink: String = "/paye/company-car/do-you-pay-towards-car/",
                                     previousTaxYearLink: String = "/check-income-tax/income-tax-history",
                                     updateEstimatedIncomeLink: String = "/check-income-tax/update-income/start",
                                     updateEmployerLink: String = "/check-income-tax/update-remove-employment/decision-page",
                                     currentYearPlusOneLink: Option[String] = Some("/check-income-tax/income-tax-comparison"),
                                     taxCodeLocation: Option[String] = None,
                                     incomeTaxHistoricPayeUrl: String = "/check-income-tax/historic-paye/",
                                     isRTIDown: Boolean = false
                                    )

object MobilePayeSummaryResponse {
  implicit val reads: Reads[MobilePayeSummaryResponse] = for {
    taxYear                   <- (__ \ "taxYear").readNullable[Int]
    employments               <- (__ \ "employments").readNullable[Seq[PayeIncome]]
    previousEmployments       <- (__ \ "previousEmployments").readNullable[Seq[PayeIncome]]
    pensions                  <- (__ \ "pensions").readNullable[Seq[PayeIncome]]
    repayment                 <- (__ \ "repayment").readNullable[P800Repayment]
    otherIncomes              <- (__ \ "otherIncomes").readNullable[Seq[OtherIncome]]
    taxCodeChange             <- (__ \ "taxCodeChange").readNullable[TaxCodeChange]
    simpleAssessment          <- (__ \ "simpleAssessment").readNullable[MobileSimpleAssessmentResponse]
    taxFreeAmount             <- (__ \ "taxFreeAmount").readNullable[BigDecimal]
    taxFreeAmountLink         <- (__ \ "taxFreeAmountLink").readNullable[String]
    estimatedTaxAmount        <- (__ \ "estimatedTaxAmount").readNullable[BigDecimal]
    estimatedTaxAmountLink    <- (__ \ "estimatedTaxAmountLink").readNullable[String]
    understandYourTaxCodeLink <- (__ \ "understandYourTaxCodeLink").readNullable[String]
    addMissingEmployerLink    <- (__ \ "addMissingEmployerLink").read[String]
    addMissingPensionLink     <- (__ \ "addMissingPensionLink").read[String]
    addMissingIncomeLink      <- (__ \ "addMissingIncomeLink").read[String]
    addMissingBenefitLink     <- (__ \ "addMissingBenefitLink").read[String]
    addMissingCompanyCarLink  <- (__ \ "addMissingCompanyCarLink").read[String]
    previousTaxYearLink       <- (__ \ "previousTaxYearLink").read[String]
    updateEstimatedIncomeLink <- (__ \ "updateEstimatedIncomeLink").read[String]
    updateEmployerLink        <- (__ \ "updateEmployerLink").read[String]
    currentYearPlusOneLink    <- (__ \ "currentYearPlusOneLink").readNullable[String]
    taxCodeLocation           <- (__ \ "taxCodeLocation").readNullable[String]
    incomeTaxHistoricPayeUrl  <- (__ \ "incomeTaxHistoricPayeUrl").read[String]
    isRTIDown                 <- (__ \ "isRTIDown").read[Boolean]

  } yield MobilePayeSummaryResponse(
    taxYear,
    employments,
    previousEmployments,
    pensions,
    repayment,
    otherIncomes,
    taxCodeChange,
    simpleAssessment,
    taxFreeAmount,
    taxFreeAmountLink,
    estimatedTaxAmount,
    estimatedTaxAmountLink,
    understandYourTaxCodeLink,
    addMissingEmployerLink,
    addMissingPensionLink,
    addMissingIncomeLink,
    addMissingBenefitLink,
    addMissingCompanyCarLink,
    previousTaxYearLink,
    updateEstimatedIncomeLink,
    updateEmployerLink,
    currentYearPlusOneLink,
    taxCodeLocation,
    incomeTaxHistoricPayeUrl,
    isRTIDown
  )

  // implicit val writes: OWrites[MobilePayeSummaryResponse] = Json.writes[MobilePayeSummaryResponse]
  implicit val writes: Writes[MobilePayeSummaryResponse] = new Writes[MobilePayeSummaryResponse] {
    def writes(response: MobilePayeSummaryResponse): JsValue = {

      // Collect fields into a Seq of tuples, filtering out None values
      val fields = Seq(
        response.taxYear.map("taxYear" -> Json.toJson(_)),
        response.employments.map("employments" -> Json.toJson(_)),
        response.previousEmployments.map("previousEmployments" -> Json.toJson(_)),
        response.pensions.map("pensions" -> Json.toJson(_)),
        response.repayment.map("repayment" -> Json.toJson(_)),
        response.otherIncomes.map("otherIncomes" -> Json.toJson(_)),
        response.taxCodeChange.map("taxCodeChange" -> Json.toJson(_)),
        response.simpleAssessment.map("simpleAssessment" -> Json.toJson(_)),
        response.taxFreeAmount.map("taxFreeAmount" -> Json.toJson(_)),
        response.taxFreeAmountLink.map("taxFreeAmountLink" -> JsString(_)),
        response.estimatedTaxAmount.map("estimatedTaxAmount" -> Json.toJson(_)),
        response.estimatedTaxAmountLink.map("estimatedTaxAmountLink" -> JsString(_)),
        response.understandYourTaxCodeLink.map("understandYourTaxCodeLink" -> JsString(_))
      ).flatten ++ Seq(
        Some("addMissingEmployerLink"    -> JsString(response.addMissingEmployerLink)),
        Some("addMissingPensionLink"     -> JsString(response.addMissingPensionLink)),
        Some("addMissingIncomeLink"      -> JsString(response.addMissingIncomeLink)),
        Some("addMissingBenefitLink"     -> JsString(response.addMissingBenefitLink)),
        Some("addMissingCompanyCarLink"  -> JsString(response.addMissingCompanyCarLink)),
        Some("previousTaxYearLink"       -> JsString(response.previousTaxYearLink)),
        Some("updateEstimatedIncomeLink" -> JsString(response.updateEstimatedIncomeLink)),
        Some("updateEmployerLink"        -> JsString(response.updateEmployerLink)),
        response.currentYearPlusOneLink.map("currentYearPlusOneLink" -> JsString(_)),
        response.taxCodeLocation.map("taxCodeLocation" -> JsString(_)),
        Some("incomeTaxHistoricPayeUrl" -> JsString(response.incomeTaxHistoricPayeUrl)),
        Some("isRTIDown"                -> JsBoolean(response.isRTIDown))
      ).flatten

      JsObject(fields)
    }
  }

  implicit val format: Format[MobilePayeSummaryResponse] = Format(reads, writes)

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

}
