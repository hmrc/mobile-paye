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
import uk.gov.hmrc.mobilepaye.connectors.TaiConnector
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.{MobilePayeResponse, OtherIncome, PayeIncome}

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

@Singleton
class MobilePayeService @Inject()(taiConnector: TaiConnector) {

  private val NpsTaxAccountNoEmploymentsCy = "no employments recorded for current tax year"
  private val NpsTaxAccountDataAbsentMsg = "cannot complete a coding calculation without a primary employment"
  private val NpsTaxAccountNoEmploymentsRecorded = "no employments recorded for this individual"

  def getMobilePayeResponse(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MobilePayeResponse] = {

    def knownException(ex: Throwable): Boolean =
      ex.getMessage.toLowerCase().contains(NpsTaxAccountNoEmploymentsCy) ||
        ex.getMessage.toLowerCase().contains(NpsTaxAccountDataAbsentMsg) ||
        ex.getMessage.toLowerCase().contains(NpsTaxAccountNoEmploymentsRecorded)


    def filterLiveIncomes(emp: TaxCodeIncome, incomeType: TaxCodeIncomeComponentType): Boolean = emp.componentType == incomeType && emp.status == Live

    def buildMobilePayeResponse(taxCodeIncomes: Seq[TaxCodeIncome],
                                nonTaxCodeIncomes: NonTaxCodeIncome,
                                employments: Seq[Employment],
                                taxAccountSummary: TaxAccountSummary): MobilePayeResponse = {

      def buildPayeIncomes(employments: Seq[Employment], taxCodeIncomes: Seq[TaxCodeIncome]): Option[Seq[PayeIncome]] = {
        employments.flatMap { emp =>
          taxCodeIncomes.filter(income => income.employmentId.fold(false) { id => id == emp.sequenceNumber }).map(tci =>
            PayeIncome(name = tci.name,
              payrollNumber = emp.payrollNumber,
              taxCode = tci.taxCode,
              amount = tci.amount.setScale(0, RoundingMode.FLOOR),
              link = Some(s"/check-income-tax/income-details/${tci.employmentId.getOrElse(throw new Exception("Employment ID not found"))}")))
        } match {
          case Nil => None
          case epi => Some(epi)
        }
      }

      val otherNonTaxCodeIncomes: Option[Seq[OtherIncome]] = nonTaxCodeIncomes.otherNonTaxCodeIncomes
        .filter(_.incomeComponentType != BankOrBuildingSocietyInterest)
        .map(income => OtherIncome.withMaybeLink(
          name = income.getFormattedIncomeComponentType,
          amount = income.amount.setScale(0, RoundingMode.FLOOR)
        )) match {
        case Nil => None
        case oi => Some(oi)
      }

      // $COVERAGE-OFF$
      //TODO We may need to use this in the future still but as part of HMA-546 to remediate a live issue this is unused until the underlying issue with untaxed interest is resolved.
      val untaxedInterest: Option[OtherIncome] = nonTaxCodeIncomes.untaxedInterest match {
        case Some(income) => Some(OtherIncome.withMaybeLink(
          name = income.getFormattedIncomeComponentType,
          amount = income.amount.setScale(0, RoundingMode.FLOOR)
        ))
        case None => None
      }

      val otherIncomes: Option[Seq[OtherIncome]] = (otherNonTaxCodeIncomes, untaxedInterest) match {
        case (Some(x), Some(y)) => Some(Seq(y) ++ x)
        case (Some(x), _) => Some(x)
        case (_, Some(y)) => Some(Seq(y))
        case _ => None
      }
      // $COVERAGE-ON$

      val liveEmployments: Seq[TaxCodeIncome] = taxCodeIncomes.filter(filterLiveIncomes(_, EmploymentIncome))
      val livePensions: Seq[TaxCodeIncome] = taxCodeIncomes.filter(filterLiveIncomes(_, PensionIncome))

      val employmentPayeIncomes: Option[Seq[PayeIncome]] = buildPayeIncomes(employments, liveEmployments)
      val pensionPayeIncomes: Option[Seq[PayeIncome]] = buildPayeIncomes(employments, livePensions)

      val taxFreeAmount: Option[BigDecimal] = Some(taxAccountSummary.taxFreeAmount.setScale(0, RoundingMode.FLOOR))
      val estimatedTaxAmount: Option[BigDecimal] = Some(taxAccountSummary.totalEstimatedTax.setScale(0, RoundingMode.FLOOR))

      MobilePayeResponse(taxYear = Some(taxYear),
        employments = employmentPayeIncomes,
        pensions = pensionPayeIncomes,
        otherIncomes = otherNonTaxCodeIncomes,
        taxFreeAmount = taxFreeAmount,
        estimatedTaxAmount = estimatedTaxAmount)
    }

    val taxCodeIncomesF = taiConnector.getTaxCodeIncomes(nino, taxYear)
    val nonTaxCodeIncomesF = taiConnector.getNonTaxCodeIncome(nino, taxYear)
    val employmentsF = taiConnector.getEmployments(nino, taxYear)
    val taxAccountSummaryF = taiConnector.getTaxAccountSummary(nino, taxYear)

    (for {
      taxCodeIncomes <- taxCodeIncomesF
      nonTaxCodeIncomes <- nonTaxCodeIncomesF
      employments <- employmentsF
      taxAccountSummary <- taxAccountSummaryF
      mobilePayeResponse: MobilePayeResponse = buildMobilePayeResponse(taxCodeIncomes, nonTaxCodeIncomes, employments, taxAccountSummary)
    } yield mobilePayeResponse) recover {
      case ex if knownException(ex) => MobilePayeResponse.empty
      case ex => throw ex
    }
  }

  def getPerson(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Person] = taiConnector.getPerson(nino)

}
