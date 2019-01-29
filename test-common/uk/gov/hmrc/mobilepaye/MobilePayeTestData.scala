/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.{IncomeSource, MobilePayeResponse, OtherIncome, PayeIncome}
import uk.gov.hmrc.time.TaxYear

trait MobilePayeTestData {

  val currentTaxYear: Int = TaxYear.current.currentYear

  val nino: Nino = Nino("CS700100A")
  val taxCodeIncome: TaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(3), "The Best Shop Ltd", 1000, Live, "S1150L")
  val taxCodeIncome2: TaxCodeIncome = taxCodeIncome.copy(name = "The Worst Shop Ltd", employmentId = Some(4))
  val taxCodeIncome3: TaxCodeIncome = taxCodeIncome.copy(componentType = PensionIncome, name = "Prestige Pensions", employmentId = Some(5))

  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(taxCodeIncome,
    taxCodeIncome2,
    taxCodeIncome3
    )

  val emptyTaxCodeIncomes: Seq[TaxCodeIncome] = Seq.empty
  val emptyEmployments: Seq[Employment] = Seq.empty

  val otherNonTaxCodeIncome: OtherNonTaxCodeIncome = OtherNonTaxCodeIncome(StatePension, BigDecimal(250.0))
  val untaxedIncome = Some(UntaxedInterest(UntaxedInterestIncome, BigDecimal(250.0)))
  val nonTaxCodeIncomeWithUntaxedInterest: NonTaxCodeIncome = NonTaxCodeIncome(untaxedIncome, Seq(otherNonTaxCodeIncome))
  val nonTaxCodeIncomeWithoutUntaxedInterest: NonTaxCodeIncome = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val taiEmployment: Employment = Employment(Some("ABC123"), 3)
  val taiEmployment2: Employment = taiEmployment.copy(payrollNumber = Some("DEF456"), sequenceNumber = 4)
  val taiEmployment3: Employment = taiEmployment.copy(payrollNumber = None, sequenceNumber = 5)

  val taiEmployments: Seq[Employment] = Seq(taiEmployment,
    taiEmployment2,
    taiEmployment3)

  val employmentIncomeSource: Seq[IncomeSource] = Seq(IncomeSource(taxCodeIncome, taiEmployment), IncomeSource(taxCodeIncome2, taiEmployment2))
  val pensionIncomeSource: Seq[IncomeSource] = Seq(IncomeSource(taxCodeIncome3, taiEmployment3))

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(BigDecimal(250), BigDecimal(10000))

  val person: Person = Person(nino, "Carrot", "Smith", None)

  val payeIncome: PayeIncome = PayeIncome("The Best Shop Ltd", Some("ABC123"), "S1150L", 1000, "/check-income-tax/income-details/3")

  val otherIncome: OtherIncome = OtherIncome("STATE PENSION", 250.0, None)
  val otherIncomeUntaxedInterest = OtherIncome("UNTAXED INTEREST INCOME", 250.0, Some("/check-income-tax/income/bank-building-society-savings"))

  val employments: Seq[PayeIncome] = Seq(payeIncome, payeIncome.copy(name = "The Worst Shop Ltd", link = "/check-income-tax/income-details/4", payrollNumber = Some("DEF456")))
  val pensions: Seq[PayeIncome] = Seq(payeIncome.copy(name = "Prestige Pensions", link = "/check-income-tax/income-details/5", payrollNumber = None))
  val otherIncomes: Seq[OtherIncome] = Seq(otherIncomeUntaxedInterest,otherIncome)

  val fullMobilePayeResponse: MobilePayeResponse = MobilePayeResponse(
    employments = Some(employments),
    pensions = Some(pensions),
    otherIncomes = Some(otherIncomes),
    taxFreeAmount = Some(10000),
    estimatedTaxAmount = Some(250)
  )
}