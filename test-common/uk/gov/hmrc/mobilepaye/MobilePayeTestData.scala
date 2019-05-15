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
import uk.gov.hmrc.mobilepaye.domain.{IncomeSource, P800Repayment, MobilePayeResponse, OtherIncome, PayeIncome}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus.{ChequeSent, PaymentPaid}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Status, P800Summary, RepaymentStatus}
import uk.gov.hmrc.time.TaxYear

trait MobilePayeTestData {
  val currentTaxYear: Int = TaxYear.current.currentYear

  val nino:           Nino          = Nino("CS700100A")
  val taxCodeIncome:  TaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(3), "The Best Shop Ltd", 1000, Live, "S1150L")
  val taxCodeIncome2: TaxCodeIncome = taxCodeIncome.copy(name = "The Worst Shop Ltd", employmentId = Some(4))
  val taxCodeIncome3: TaxCodeIncome = taxCodeIncome.copy(componentType = PensionIncome, name = "Prestige Pensions", employmentId = Some(5))

  val otherNonTaxCodeIncome: OtherNonTaxCodeIncome   = OtherNonTaxCodeIncome(StatePension, BigDecimal(250.0))
  val untaxedIncome:         Option[UntaxedInterest] = Some(UntaxedInterest(UntaxedInterestIncome, BigDecimal(250.0)))
  val nonTaxCodeIncome:      NonTaxCodeIncome        = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val nonTaxCodeIncomeWithUntaxedInterest:    NonTaxCodeIncome = NonTaxCodeIncome(untaxedIncome, Seq(otherNonTaxCodeIncome))
  val nonTaxCodeIncomeWithoutUntaxedInterest: NonTaxCodeIncome = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val taiEmployment:  Employment = Employment(Some("ABC123"), 3)
  val taiEmployment2: Employment = taiEmployment.copy(payrollNumber = Some("DEF456"), sequenceNumber = 4)
  val taiEmployment3: Employment = taiEmployment.copy(payrollNumber = None, sequenceNumber = 5)

  val employmentIncomeSource: Seq[IncomeSource] = Seq(IncomeSource(taxCodeIncome, taiEmployment), IncomeSource(taxCodeIncome2, taiEmployment2))
  val pensionIncomeSource:    Seq[IncomeSource] = Seq(IncomeSource(taxCodeIncome3, taiEmployment3))

  val employments: Seq[PayeIncome] = employmentIncomeSource.map(ic => PayeIncome.fromIncomeSource(ic))
  val pensions:    Seq[PayeIncome] = pensionIncomeSource.map(ic => PayeIncome.fromIncomeSource(ic))

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(BigDecimal(250), BigDecimal(10000))
  val person:            Person            = Person(nino, "Carrot", "Smith", None)
  val otherIncome:       OtherIncome       = OtherIncome("STATE PENSION", 250.0, None)

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

  val employmentsNoLinks: Seq[PayeIncome]  = employments.map(employment => employment.copy(link = None, payrollNumber = None))
  val pensionsNoLinks:    Seq[PayeIncome]  = pensions.map(pension => pension.copy(link = None, payrollNumber = None))
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
