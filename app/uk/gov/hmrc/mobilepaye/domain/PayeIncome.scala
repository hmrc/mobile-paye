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
import uk.gov.hmrc.mobilepaye.domain.tai.Payment

import scala.math.BigDecimal.RoundingMode

// Link is only optional to remove it for auditing purposes, it's a mandatory data field, we could consider different classes for auditing instead
case class PayeIncome(
  name:          String,
  payrollNumber: Option[String] = None,
  taxCode:       String,
  amount:        BigDecimal,
  link:          Option[String],
  payments:      Seq[Payment])

object PayeIncome {

  def fromIncomeSource(incomeSource: IncomeSource): PayeIncome =
    PayeIncome(
      name          = incomeSource.taxCodeIncome.name,
      payrollNumber = incomeSource.employment.payrollNumber,
      taxCode       = incomeSource.taxCodeIncome.taxCode,
      amount        = incomeSource.taxCodeIncome.amount.setScale(0, RoundingMode.FLOOR),
      link = Option
      (
        s"/check-income-tax/income-details/${incomeSource.taxCodeIncome.employmentId.getOrElse(throw new Exception("Employment ID not found"))}"
      ),
      payments = incomeSource.employment.annualAccounts.headOption.map(accounts => accounts.payments).getOrElse(Seq.empty)
    )

  implicit val format: OFormat[PayeIncome] = Json.format[PayeIncome]
}
