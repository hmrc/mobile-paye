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

package uk.gov.hmrc.mobilepaye

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.{MobilePayeResponse, OtherIncome, PayeIncome}
import uk.gov.hmrc.time.TaxYear

trait MobilePayeTestData {

  val currentTaxYear = TaxYear.current.currentYear

  val nino = Nino("CS700100A")
  val taxCodeIncome: TaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(3), 1000, "S1150L")
  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(taxCodeIncome, taxCodeIncome.copy(employmentId = Some(4)))

  val emptyTaxCodeIncomes: Seq[TaxCodeIncome] = Seq.empty
  val emptyEmployments: Seq[Employment] = Seq.empty

  val otherNonTaxCodeIncome: OtherNonTaxCodeIncome = OtherNonTaxCodeIncome(OtherIncomeEarned, BigDecimal(100.0))
  val nonTaxCodeIncome: NonTaxCodeIncome = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val taiEmployment: Employment = Employment(Some("payrollNum"), 2)
  val taiEmployments: Seq[Employment] = Seq(taiEmployment, taiEmployment.copy(payrollNumber = Some("anotherPayrollNum"), sequenceNumber = 3))

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(BigDecimal(200.0), BigDecimal(123.12))

  val person: Person = Person(nino, "Carrot", "Smith", None)

  val payeIncome: PayeIncome = PayeIncome("Sainsburys", None, "S1150L", 1000, "/some/link")

  val employments: Seq[PayeIncome] = Seq(payeIncome, payeIncome.copy(name = "Tesco", amount = 500))
  val pensions: Seq[PayeIncome] = Seq(payeIncome.copy(name = "Prestige Pensions"))
  val otherIncomes: Seq[OtherIncome] = Seq(OtherIncome("SomeOtherIncome", 250))

  val fullResponse = MobilePayeResponse(
    employments = Some(employments),
    pensions = Some(pensions),
    otherIncomes = Some(otherIncomes),
    taxFreeAmount = 10000,
    estimatedTaxAmount = 250
  )

}