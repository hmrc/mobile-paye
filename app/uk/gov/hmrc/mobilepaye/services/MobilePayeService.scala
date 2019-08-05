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

package uk.gov.hmrc.mobilepaye.services

import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.connectors.{TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.IncomeSource
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Summary
import uk.gov.hmrc.mobilepaye.domain.{MobilePayeResponse, OtherIncome, P800Repayment, PayeIncome}

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

@Singleton
class MobilePayeService @Inject()(taiConnector: TaiConnector, taxCalcConnector: TaxCalcConnector) {

  private val NpsTaxAccountNoEmploymentsCurrentYear = "no employments recorded for current tax year"
  private val NpsTaxAccountDataAbsentMsg            = "cannot complete a coding calculation without a primary employment"
  private val NpsTaxAccountNoEmploymentsRecorded    = "no employments recorded for this individual"

  def getMobilePayeResponse(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MobilePayeResponse] = {

    def knownException(ex: Throwable): Boolean =
      ex.getMessage.toLowerCase().contains(NpsTaxAccountNoEmploymentsCurrentYear) ||
        ex.getMessage.toLowerCase().contains(NpsTaxAccountDataAbsentMsg) ||
        ex.getMessage.toLowerCase().contains(NpsTaxAccountNoEmploymentsRecorded)

    def buildMobilePayeResponse(
      incomeSourceEmployment: Seq[IncomeSource],
      incomeSourcePension:    Seq[IncomeSource],
      nonTaxCodeIncomes:      NonTaxCodeIncome,
      taxAccountSummary:      TaxAccountSummary,
      p800Summary:            Option[P800Summary]): MobilePayeResponse = {

      def buildPayeIncomes(incomes: Seq[IncomeSource]): Option[Seq[PayeIncome]] =
        incomes.map { inc => PayeIncome.fromIncomeSource(inc)
        } match {
          case Nil => None
          case epi => Some(epi)
        }

      val otherNonTaxCodeIncomes: Option[Seq[OtherIncome]] = nonTaxCodeIncomes.otherNonTaxCodeIncomes
        .filter(_.incomeComponentType != BankOrBuildingSocietyInterest)
        .map(
          income =>
            OtherIncome.withMaybeLink(
              name   = income.getFormattedIncomeComponentType,
              amount = income.amount.setScale(0, RoundingMode.FLOOR)
          )) match {
        case Nil => None
        case oi  => Some(oi)
      }

      // $COVERAGE-OFF$
      //TODO We may need to use this in the future still but as part of HMA-546 to remediate a live issue this is unused until the underlying issue with untaxed interest is resolved.
      val untaxedInterest: Option[OtherIncome] = nonTaxCodeIncomes.untaxedInterest match {
        case Some(income) =>
          Some(
            OtherIncome.withMaybeLink(
              name   = income.getFormattedIncomeComponentType,
              amount = income.amount.setScale(0, RoundingMode.FLOOR)
            ))
        case None => None
      }

      val otherIncomes: Option[Seq[OtherIncome]] = (otherNonTaxCodeIncomes, untaxedInterest) match {
        case (Some(x), Some(y)) => Some(Seq(y) ++ x)
        case (Some(x), _)       => Some(x)
        case (_, Some(y))       => Some(Seq(y))
        case _                  => None
      }
      // $COVERAGE-ON$

      val employmentPayeIncomes: Option[Seq[PayeIncome]] = buildPayeIncomes(incomeSourceEmployment)
      val pensionPayeIncomes:    Option[Seq[PayeIncome]] = buildPayeIncomes(incomeSourcePension)

      val taxFreeAmount:      Option[BigDecimal]    = Option(taxAccountSummary.taxFreeAmount.setScale(0, RoundingMode.FLOOR))
      val estimatedTaxAmount: Option[BigDecimal]    = Option(taxAccountSummary.totalEstimatedTax.setScale(0, RoundingMode.FLOOR))
      val repayment:          Option[P800Repayment] = p800Summary.flatMap(summary => P800Summary.toP800Repayment(summary))

      MobilePayeResponse(
        taxYear             = Some(taxYear),
        employments         = employmentPayeIncomes,
        repayment           = repayment,
        pensions            = pensionPayeIncomes,
        otherIncomes        = otherNonTaxCodeIncomes,
        taxFreeAmount       = taxFreeAmount,
        estimatedTaxAmount  = estimatedTaxAmount,
        previousTaxYearLink = Some(s"/check-income-tax/historic-paye/${taxYear - 1}")
      )
    }

    (for {
      taxCodeIncomesEmployment <- taiConnector.getMatchingTaxCodeIncomes(nino, taxYear, EmploymentIncome.toString, Live.toString)
      taxCodeIncomesPension    <- taiConnector.getMatchingTaxCodeIncomes(nino, taxYear, PensionIncome.toString, Live.toString)
      nonTaxCodeIncomes        <- taiConnector.getNonTaxCodeIncome(nino, taxYear)
      taxAccountSummary        <- taiConnector.getTaxAccountSummary(nino, taxYear)
      p800Summary              <- taxCalcConnector.getP800Summary(nino, taxYear)
      mobilePayeResponse: MobilePayeResponse = buildMobilePayeResponse(
        taxCodeIncomesEmployment,
        taxCodeIncomesPension,
        nonTaxCodeIncomes,
        taxAccountSummary,
        p800Summary)
    } yield mobilePayeResponse) recover {
      case ex if knownException(ex) => MobilePayeResponse.empty
      case ex                       => throw ex
    }
  }

  def getPerson(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Person] = taiConnector.getPerson(nino)

}
