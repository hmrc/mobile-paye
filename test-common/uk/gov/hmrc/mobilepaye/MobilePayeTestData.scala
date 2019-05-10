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

import java.time.LocalDate
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus.{ChequeSent, PaymentPaid}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Status, P800Summary, RepaymentStatus}
import uk.gov.hmrc.mobilepaye.domain.{MobilePayeResponse, OtherIncome, P800Repayment, PayeIncome}
import uk.gov.hmrc.time.TaxYear

trait MobilePayeTestData {

  val currentTaxYear: Int = TaxYear.current.currentYear

  val nino:          Nino          = Nino("CS700100A")
  val taxCodeIncome: TaxCodeIncome = TaxCodeIncome(TaxCodeIncomeComponentType.EmploymentIncome, Some(3), "The Best Shop Ltd", 1000, TaxCodeIncomeStatus.Live, "S1150L")

  val taxCodeIncomes: Seq[TaxCodeIncome] = Seq(
    taxCodeIncome,
    taxCodeIncome.copy(name          = "The Worst Shop Ltd", employmentId = Some(4)),
    taxCodeIncome.copy(componentType = TaxCodeIncomeComponentType.PensionIncome, name = "Prestige Pensions", employmentId = Some(5))
  )

  val emptyTaxCodeIncomes: Seq[TaxCodeIncome] = Seq.empty
  val emptyEmployments:    Seq[Employment]    = Seq.empty

  val otherNonTaxCodeIncome: OtherNonTaxCodeIncome = OtherNonTaxCodeIncome(NonTaxCodeIncomeComponentType.StatePension, BigDecimal(250.0))
  val untaxedIncome = Some(UntaxedInterest(NonTaxCodeIncomeComponentType.UntaxedInterestIncome, BigDecimal(250.0)))
  val nonTaxCodeIncome:    NonTaxCodeIncome = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val taiEmployment: Employment = Employment(Some("ABC123"), 3)
  val taiEmployments: Seq[Employment] = Seq(
    taiEmployment,
    taiEmployment.copy(payrollNumber = Some("DEF456"), sequenceNumber = 4),
    taiEmployment.copy(payrollNumber = None, sequenceNumber           = 5))

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(BigDecimal(250), BigDecimal(10000))

  val person: Person = Person(nino, "Carrot", "Smith", None)

  val payeIncome: PayeIncome = PayeIncome("The Best Shop Ltd", Some("ABC123"), "S1150L", 1000, Some("/check-income-tax/income-details/3"))

  val otherIncome: OtherIncome = OtherIncome("STATE PENSION", 250.0, None)
  val otherIncomeUntaxedInterest = OtherIncome("UNTAXED INTEREST INCOME", 250.0, Some("/check-income-tax/income/bank-building-society-savings"))

  def repayment(p800Status: P800Status, paymentStatus: RepaymentStatus, taxYear: Int, amount: BigDecimal, time: LocalDate): Option[P800Repayment] = {
    def withPaidDate(): Option[LocalDate] = {
      paymentStatus match {
        case PaymentPaid | ChequeSent => Option(LocalDate.from(time))
        case _                        => None
      }
    }

    val summary = P800Summary(p800Status, paymentStatus, amount, taxYear, withPaidDate())

    P800Summary.toP800Repayment(summary)
  }

  val employments: Seq[PayeIncome] =
    Seq(payeIncome, payeIncome.copy(name = "The Worst Shop Ltd", link = Some("/check-income-tax/income-details/4"), payrollNumber = Some("DEF456")))
  val pensions: Seq[PayeIncome] =
    Seq(payeIncome.copy(name = "Prestige Pensions", link = Some("/check-income-tax/income-details/5"), payrollNumber = None))
  val otherIncomes: Seq[OtherIncome] = Seq(otherIncome)

  val fullMobilePayeResponse: MobilePayeResponse = MobilePayeResponse(
    taxYear            = Some(TaxYear.current.currentYear),
    employments        = Some(employments),
    repayment          = None,
    pensions           = Some(pensions),
    otherIncomes       = Some(otherIncomes),
    taxFreeAmount      = Some(10000),
    estimatedTaxAmount = Some(250)
  )

  val employmentsNoLinks: Seq[PayeIncome] =
    Seq(payeIncome.copy(link = None), payeIncome.copy(name = "The Worst Shop Ltd", link = None, payrollNumber = Some("DEF456")))
  val pensionsNoLinks: Seq[PayeIncome] =
    Seq(payeIncome.copy(name = "Prestige Pensions", link = None, payrollNumber = None))
  val otherIncomeNoLinks: Seq[OtherIncome] = Seq(otherIncome)

  val fullMobilePayeAudit: MobilePayeResponse = fullMobilePayeResponse.copy(
    employments               = Some(employmentsNoLinks),
    pensions                  = Some(pensionsNoLinks),
    otherIncomes              = Some(otherIncomeNoLinks),
    taxFreeAmountLink         = None,
    estimatedTaxAmountLink    = None,
    understandYourTaxCodeLink = None,
    addMissingEmployerLink    = None,
    addMissingPensionLink     = None,
    addMissingIncomeLink      = None
  )
}
