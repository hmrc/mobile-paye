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

import play.api.libs.json.{Json, OFormat}

case class MobilePayePreviousYearSummaryResponse(
  taxYear:                   Option[Int],
  employments:               Option[Seq[PayeIncome]],
  previousEmployments:       Option[Seq[PayeIncome]],
  pensions:                  Option[Seq[PayeIncome]],
  otherIncomes:              Option[Seq[OtherIncome]],
  taxFreeAmount:             Option[BigDecimal],
  estimatedTaxAmount:        Option[BigDecimal],
  understandYourTaxCodeLink: String = "/check-income-tax/tax-codes",
  payeSomethingWrongLink:    String)

object MobilePayePreviousYearSummaryResponse {

  implicit val format: OFormat[MobilePayePreviousYearSummaryResponse] =
    Json.format[MobilePayePreviousYearSummaryResponse]
}
