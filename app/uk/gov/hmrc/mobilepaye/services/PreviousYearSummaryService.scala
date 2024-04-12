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

package uk.gov.hmrc.mobilepaye.services

import com.google.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.mobilepaye.connectors.TaiConnector
import uk.gov.hmrc.mobilepaye.domain.{MobilePayePreviousYearSummaryResponse, OtherIncome, PayeIncome}
import uk.gov.hmrc.mobilepaye.domain.tai.{BankOrBuildingSocietyInterest, Benefits, Employment, Live, NonTaxCodeIncome, TaxAccountSummary, TaxCodeRecord}
import uk.gov.hmrc.time.TaxYear

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

@Singleton
class PreviousYearSummaryService @Inject() (
  taiConnector:                                                                  TaiConnector,
  @Named("numberOfPreviousYearsToShowPayeSummary") previousYearPayeSummaryYears: Int) {

  val logger: Logger = Logger(this.getClass)

  def getMobilePayePreviousYearSummaryResponse(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[MobilePayePreviousYearSummaryResponse] =
    if (taxYear < TaxYear.current.currentYear - previousYearPayeSummaryYears) {
      logger.warn(
        s"Tax Year requested ($taxYear) is older than the current limit of $previousYearPayeSummaryYears years"
      )
      throw new NotFoundException("No data available")
    } else
      (for {
        allEmploymentData  <- taiConnector.getEmployments(nino, taxYear)
        nonTaxCodeIncomes  <- taiConnector.getNonTaxCodeIncome(nino, taxYear)
        taxAccountSummary  <- taiConnector.getTaxAccountSummary(nino, taxYear)
        employmentBenefits <- taiConnector.getBenefits(nino, taxYear)
        taxCodes           <- taiConnector.getTaxCodesForYear(nino, taxYear)
      } yield {
        buildMobilePayePreviousYearResponse(taxYear,
                                            allEmploymentData,
                                            nonTaxCodeIncomes,
                                            taxAccountSummary,
                                            employmentBenefits,
                                            taxCodes)
      }) recover {
        case ex =>
          logger.warn(s"Error retrieving previous PAYE summary info: ${ex.printStackTrace()}")
          throw new NotFoundException(ex.getMessage)
      }

  private def buildMobilePayePreviousYearResponse(
    taxYear:            Int,
    employmentData:     Seq[Employment],
    nonTaxCodeIncomes:  NonTaxCodeIncome,
    taxAccountSummary:  TaxAccountSummary,
    employmentBenefits: Benefits,
    taxCodes:           Seq[TaxCodeRecord]
  ): MobilePayePreviousYearSummaryResponse = {

    val liveEmployments =
      employmentData.filter(emp => emp.employmentStatus.equals(Live) && !emp.receivingOccupationalPension)
    val notLiveEmployments =
      employmentData.filter(emp => !emp.employmentStatus.equals(Live) && !emp.receivingOccupationalPension)
    val pensions =
      employmentData.filter(_.receivingOccupationalPension)

    val otherNonTaxCodeIncomes: Option[Seq[OtherIncome]] = nonTaxCodeIncomes.otherNonTaxCodeIncomes
      .filter(_.incomeComponentType != BankOrBuildingSocietyInterest)
      .map(income =>
        OtherIncome.withMaybeLink(
          name   = income.getFormattedIncomeComponentType,
          amount = income.amount.setScale(0, RoundingMode.FLOOR)
        )
      ) match {
      case Nil => None
      case oi  => Some(oi)
    }

    val taxFreeAmount: Option[BigDecimal] = Option(taxAccountSummary.taxFreeAmount.setScale(0, RoundingMode.FLOOR))
    val estimatedTaxAmount: Option[BigDecimal] = Option(
      taxAccountSummary.totalEstimatedTax.setScale(0, RoundingMode.FLOOR)
    )

    MobilePayePreviousYearSummaryResponse(
      taxYear                = Some(taxYear),
      employments            = buildPayeIncomes(liveEmployments, taxCodes, Some(employmentBenefits), taxYear),
      previousEmployments    = buildPayeIncomes(notLiveEmployments, taxCodes, Some(employmentBenefits), taxYear),
      pensions               = buildPayeIncomes(pensions, taxCodes, None, taxYear),
      otherIncomes           = otherNonTaxCodeIncomes,
      taxFreeAmount          = taxFreeAmount,
      estimatedTaxAmount     = estimatedTaxAmount,
      payeSomethingWrongLink = s"/check-income-tax/update-income-details/decision/$taxYear"
    )
  }

  private def buildPayeIncomes(
    incomes:            Seq[Employment],
    taxCodes:           Seq[TaxCodeRecord],
    employmentBenefits: Option[Benefits],
    taxYear:            Int
  ): Option[Seq[PayeIncome]] =
    incomes.map { inc =>
      PayeIncome.fromEmployment(inc, findTaxCode(inc, taxCodes), employmentBenefits, taxYear)
    } match {
      case Nil => None
      case epi => Some(epi)
    }

  private def findTaxCode(
    emp:      Employment,
    taxCodes: Seq[TaxCodeRecord]
  ): Option[String] =
    taxCodes.find(_.employerName == emp.name).map(_.taxCode)
}
