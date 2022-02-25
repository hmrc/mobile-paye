/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.mobilepaye.domain.{IncomeSource, MobilePayeResponse, MobilePayeResponseAudit, OtherIncome, OtherIncomeAudit, P800Repayment, PayeIncome, PayeIncomeAudit}
import uk.gov.hmrc.mobilepaye.domain.tai.{Payment, _}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus.{ChequeSent, PaymentPaid}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Status, P800Summary, RepaymentStatus}
import uk.gov.hmrc.time.TaxYear

trait MobilePayeTestData {
  val currentTaxYear: Int = TaxYear.current.startYear
  val endOfTaxYear:   Int = TaxYear.current.finishYear

  val nino:           Nino          = Nino("CS700100A")
  val taxCodeIncome:  TaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(3), "The Best Shop Ltd", 1000, "S1150L")
  val taxCodeIncome2: TaxCodeIncome = taxCodeIncome.copy(name = "The Worst Shop Ltd", employmentId = Some(4))

  val taxCodeIncome3: TaxCodeIncome =
    taxCodeIncome.copy(componentType = PensionIncome, name = "Prestige Pensions", employmentId = Some(5))

  val otherNonTaxCodeIncome: OtherNonTaxCodeIncome   = OtherNonTaxCodeIncome(StatePension, BigDecimal(250.0))
  val untaxedIncome:         Option[UntaxedInterest] = Some(UntaxedInterest(UntaxedInterestIncome, BigDecimal(250.0)))
  val nonTaxCodeIncome:      NonTaxCodeIncome        = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val nonTaxCodeIncomeWithUntaxedInterest: NonTaxCodeIncome =
    NonTaxCodeIncome(untaxedIncome, Seq(otherNonTaxCodeIncome))
  val nonTaxCodeIncomeWithoutUntaxedInterest: NonTaxCodeIncome = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val payments: Seq[Payment] = Seq(
    Payment(LocalDate.now(), 100, 20, 10, 50, 5, 2),
    Payment(LocalDate.now().minusDays(10), 80, 20, 10, 20, 5, 2),
    Payment(LocalDate.now().minusDays(20), 50, 20, 10, 30, 5, 2)
  )

  val payments2: Seq[Payment] = Seq(
    Payment(LocalDate.now().plusDays(1), 100, 20, 10, 50, 5, 2),
    Payment(LocalDate.now().minusDays(10), 80, 20, 10, 20, 5, 2),
    Payment(LocalDate.now().minusDays(20), 50, 20, 10, 30, 5, 2)
  )

  val annualAccount:  AnnualAccount = AnnualAccount(payments)
  val annualAccount2: AnnualAccount = AnnualAccount(payments2)

  val taiEmployment: Employment = Employment(Some("ABC123"), 3, Seq(annualAccount))

  val taiEmployment2: Employment = taiEmployment.copy(
    payrollNumber  = Some("DEF456"),
    sequenceNumber = 4,
    annualAccounts = Seq(AnnualAccount(Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2))))
  )

  val taiEmployment3: Employment =
    taiEmployment.copy(payrollNumber = None, sequenceNumber = 5, annualAccounts = Seq(annualAccount))

  val taiEmployment4: Employment =
    taiEmployment.copy(payrollNumber = None, sequenceNumber = 5, annualAccounts = Seq(annualAccount2))

  val taiEmployment5: Employment = taiEmployment.copy(
    payrollNumber  = Some("DEF456"),
    sequenceNumber = 4,
    annualAccounts = Seq(AnnualAccount(Seq.empty))
  )

  val employmentIncomeSource: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome, taiEmployment), IncomeSource(taxCodeIncome2, taiEmployment2))

  val employmentIncomeSource2: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome, taiEmployment4))

  val employmentIncomeSourceNoPayments: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome, taiEmployment5))

  val employmentIncomeSourceWelsh: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome.copy(taxCode = "C1150L"), taiEmployment),
        IncomeSource(taxCodeIncome2, taiEmployment2))

  val employmentIncomeSourceUK: Seq[IncomeSource] =
    Seq(IncomeSource(taxCodeIncome.copy(taxCode = "1150L"), taiEmployment),
        IncomeSource(taxCodeIncome2, taiEmployment2))
  val pensionIncomeSource: Seq[IncomeSource] = Seq(IncomeSource(taxCodeIncome3, taiEmployment3))

  val pensionIncomeSourceNoPension: Seq[IncomeSource] = Seq.empty

  val employments: Seq[PayeIncome] =
    employmentIncomeSource.map(ic => PayeIncome.fromIncomeSource(ic, employment = true))

  val welshEmployments: Seq[PayeIncome] =
    employmentIncomeSourceWelsh.map(ic => PayeIncome.fromIncomeSource(ic, employment = true))

  val ukEmployments: Seq[PayeIncome] =
    employmentIncomeSourceUK.map(ic => PayeIncome.fromIncomeSource(ic, employment = true))

  val pensions: Seq[PayeIncome] =
    pensionIncomeSource.map(ic => PayeIncome.fromIncomeSource(ic, employment = false))

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(BigDecimal(250), BigDecimal(10000))
  val person:            Person            = Person(nino, "Carrot", "Smith", None)
  val otherIncome:       OtherIncome       = OtherIncome("STATE PENSION", 250.0, None)

  def repayment(
    p800Status:    P800Status,
    paymentStatus: RepaymentStatus,
    taxYear:       Int,
    amount:        BigDecimal,
    time:          LocalDate
  ): Option[P800Repayment] = {
    def withPaidDate(): Option[LocalDate] =
      paymentStatus match {
        case PaymentPaid | ChequeSent => Option(LocalDate.from(time))
        case _                        => None
      }

    val summary = P800Summary(p800Status, Some(paymentStatus), Some(amount), withPaidDate())

    P800Summary.toP800Repayment(summary, taxYear)
  }

  val otherIncomes: Seq[OtherIncome] = Seq(otherIncome)

  val fullMobilePayeResponse: MobilePayeResponse = MobilePayeResponse(
    taxYear                = Some(TaxYear.current.currentYear),
    employments            = Some(employments),
    repayment              = None,
    pensions               = Some(pensions),
    otherIncomes           = Some(otherIncomes),
    taxFreeAmount          = Some(10000),
    estimatedTaxAmount     = Some(250),
    previousTaxYearLink    = s"/check-income-tax/historic-paye/${TaxYear.current.currentYear - 1}",
    currentYearPlusOneLink = None
  )

  val fullMobilePayeResponseWithCY1Link: MobilePayeResponse = MobilePayeResponse(
    taxYear             = Some(TaxYear.current.currentYear),
    employments         = Some(employments),
    repayment           = None,
    pensions            = Some(pensions),
    otherIncomes        = Some(otherIncomes),
    taxFreeAmount       = Some(10000),
    estimatedTaxAmount  = Some(250),
    previousTaxYearLink = s"/check-income-tax/historic-paye/${TaxYear.current.currentYear - 1}",
    taxCodeLocation     = Some("Scottish")
  )

  val fullMobilePayeAudit: MobilePayeResponseAudit = MobilePayeResponseAudit(
    taxYear = Some(TaxYear.current.currentYear),
    employments =
      Some(employments.map(employment => PayeIncomeAudit.fromPayeIncome(employment.copy(payrollNumber = None)))),
    repayment          = None,
    pensions           = Some(pensions.map(pension => PayeIncomeAudit.fromPayeIncome(pension.copy(payrollNumber = None)))),
    otherIncomes       = Some(otherIncomes.map(otherIncs => OtherIncomeAudit.fromOtherIncome((otherIncs)))),
    taxFreeAmount      = Some(10000),
    estimatedTaxAmount = Some(250)
  )
}
