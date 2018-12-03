/*
 * Copyright 2018 HM Revenue & Customs
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

case class MobilePayeResponse(employments: Option[Seq[PayeIncome]],
                              pensions: Option[Seq[PayeIncome]],
                              otherIncomes: Option[Seq[OtherIncome]],
                              taxFreeAmount: Int,
                              taxFreeAmountLink: String = "https://www.tax.service.gov.uk/check-income-tax/tax-free-allowance",
                              estimatedTaxAmount: Int,
                              estimatedTaxAmountLink: String = "https://www.tax.service.gov.uk/check-income-tax/paye-income-tax-estimate",
                              understandYourTaxCodeLink: String = "https://www.tax.service.gov.uk/check-income-tax/tax-codes",
                              addMissingEmployerLink: String = "https://www.tax.service.gov.uk/check-income-tax/add-employment/employment-name",
                              addMissingPensionLink: String = "https://www.tax.service.gov.uk/check-income-tax/add-pension-provider/name",
                              addMissingIncomeLink: String = "https://www.tax.service.gov.uk/forms/form/tell-us-about-other-income/guide")

object MobilePayeResponse {
  implicit val format: OFormat[MobilePayeResponse] = Json.format[MobilePayeResponse]
}


