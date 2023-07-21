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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.mobilepaye.connectors.{FeedbackConnector, TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Summary, TaxYearReconciliation}
import uk.gov.hmrc.mobilepaye.domain.{Feedback, IncomeSource, MobilePayePreviousYearSummaryResponse, MobilePayeSummaryResponse, OtherIncome, P800Cache, P800Repayment, PayeIncome, TaxCodeChange}
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
import uk.gov.hmrc.time.TaxYear

import java.time.{LocalDateTime, ZoneId}
import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

@Singleton
class MobilePayeService @Inject() (
  taiConnector:                                                                  TaiConnector,
  taxCalcConnector:                                                              TaxCalcConnector,
  p800CacheMongo:                                                                P800CacheMongo,
  feedbackConnector:                                                             FeedbackConnector,
  @Named("rUK.startDate") rUKComparisonStartDate:                                String,
  @Named("rUK.endDate") rUKComparisonEndDate:                                    String,
  @Named("wales.startDate") walesComparisonStartDate:                            String,
  @Named("wales.endDate") walesComparisonEndDate:                                String,
  @Named("scotland.startDate") scotlandComparisonStartDate:                      String,
  @Named("scotland.endDate") scotlandComparisonEndDate:                          String,
  @Named("p800CacheEnabled") p800CacheEnabled:                                   Boolean,
  @Named("taxCodeChangeEnabled") taxCodeChangeEnabled:                           Boolean,
  @Named("previousEmploymentsEnabled") previousEmploymentsEnabled:               Boolean,
  @Named("numberOfPreviousYearsToShowPayeSummary") previousYearPayeSummaryYears: Int) {

  private val NpsTaxAccountNoEmploymentsCurrentYear = "no employments recorded for current tax year"
  private val NpsTaxAccountDataAbsentMsg            = "cannot complete a coding calculation without a primary employment"
  private val NpsTaxAccountNoEmploymentsRecorded    = "no employments recorded for this individual"
  val logger: Logger = Logger(this.getClass)

  def getMobilePayeSummaryResponse(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[MobilePayeSummaryResponse] =
    (for {
      taxCodeIncomesEmployment <- taiConnector
                                   .getMatchingTaxCodeIncomes(nino, taxYear, EmploymentIncome.toString, Live.toString)
      previousEmployments <- getPreviousEmployments(nino, taxYear)
      taxCodeIncomesPension <- taiConnector
                                .getMatchingTaxCodeIncomes(nino, taxYear, PensionIncome.toString, Live.toString)
      nonTaxCodeIncomes        <- taiConnector.getNonTaxCodeIncome(nino, taxYear)
      taxAccountSummary        <- taiConnector.getTaxAccountSummary(nino, taxYear)
      reconciliations          <- getTaxYearReconciliationsForP800(nino)
      tcComparisonPeriodActive <- cyPlus1InfoCheck(taxCodeIncomesEmployment)
      cy1InfoAvailable <- if (tcComparisonPeriodActive) taiConnector.getCYPlusOneAccountSummary(nino, taxYear)
                         else Future successful false
      employmentBenefits <- taiConnector.getBenefits(nino, taxYear)
      taxCodeChangeExists <- if (taxCodeChangeEnabled) taiConnector.getTaxCodeChangeExists(nino)
                            else Future successful false
      mobilePayeResponse: MobilePayeSummaryResponse = buildMobilePayeResponse(
        nino,
        taxYear,
        taxCodeIncomesEmployment,
        previousEmployments,
        taxCodeIncomesPension,
        nonTaxCodeIncomes,
        taxAccountSummary,
        getP800Summary(reconciliations, taxYear),
        employmentBenefits,
        Some(TaxCodeChange(taxCodeChangeExists))
      )
    } yield {
      if (cy1InfoAvailable) mobilePayeResponse.copy(taxCodeLocation = getTaxCodeLocation(taxCodeIncomesEmployment))
      else mobilePayeResponse.copy(currentYearPlusOneLink           = None)
    }) recover {
      case ex if knownException(ex, nino) => MobilePayeSummaryResponse.empty
      case ex                             => throw ex
    }

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
        taxCodeIncomesEmployment <- taiConnector
                                     .getMatchingTaxCodeIncomes(nino, taxYear, EmploymentIncome.toString, Live.toString)
        previousEmployments <- getPreviousEmployments(nino, taxYear)
        taxCodeIncomesPension <- taiConnector
                                  .getMatchingTaxCodeIncomes(nino, taxYear, PensionIncome.toString, Live.toString)
        nonTaxCodeIncomes  <- taiConnector.getNonTaxCodeIncome(nino, taxYear)
        taxAccountSummary  <- taiConnector.getTaxAccountSummary(nino, taxYear)
        employmentBenefits <- taiConnector.getBenefits(nino, taxYear)
        mobilePayeResponse: MobilePayeSummaryResponse = buildMobilePayeResponse(
          nino,
          taxYear,
          taxCodeIncomesEmployment,
          previousEmployments,
          taxCodeIncomesPension,
          nonTaxCodeIncomes,
          taxAccountSummary,
          None,
          employmentBenefits,
          None
        )
      } yield {
        MobilePayePreviousYearSummaryResponse.fromPayeSummaryResponse(mobilePayeResponse)
      }) recover {
        case ex =>
          logger.warn(s"Error retrieving previous PAYE summary info: ${ex.printStackTrace()}")
          throw new NotFoundException(ex.getMessage)
      }

  def getPerson(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Person] = taiConnector.getPerson(nino)

  private def getP800Summary(
    reconciliations: Option[List[TaxYearReconciliation]],
    taxYear:         Int
  ): Option[P800Summary] = {
    val previousYear = taxYear - 1
    reconciliations match {
      case None => None
      case _    => reconciliations.get.find(recon => recon.taxYear == previousYear).map(_.reconciliation)

    }
  }

  private def getTaxYearReconciliationsForP800(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Option[List[TaxYearReconciliation]]] =
    if (p800CacheEnabled) {
      val cacheCheckResult = p800CacheMongo.selectByNino(nino)
      cacheCheckResult.flatMap(recordFound =>
        if (recordFound.isEmpty) taxCalcConnector.getTaxReconciliations(nino) else Future.successful(None)
      )
    } else taxCalcConnector.getTaxReconciliations(nino)

  private def knownException(
    ex:   Throwable,
    nino: Nino
  ): Boolean = {
    val known = ex.getMessage.toLowerCase().contains(NpsTaxAccountNoEmploymentsCurrentYear) ||
      ex.getMessage.toLowerCase().contains(NpsTaxAccountDataAbsentMsg) ||
      ex.getMessage.toLowerCase().contains(NpsTaxAccountNoEmploymentsRecorded)
    logger.info(s"[HMA-2505] - Tai exception (known: $known) for ${nino.nino} - " + ex.getMessage.toLowerCase())
    known
  }

  private def getAndCacheP800RepaymentCheck(
    nino:        Nino,
    taxYear:     Int,
    p800Summary: Option[P800Summary]
  ): Option[P800Repayment] = {
    val repayment = p800Summary.flatMap(summary => P800Summary.toP800Repayment(summary, taxYear))
    repayment match {
      case None => {
        if (p800CacheEnabled) {
          p800CacheMongo.add(P800Cache(nino))
        }
        None
      }
      case _ => repayment
    }
  }

  private def buildMobilePayeResponse(
    nino:                   Nino,
    taxYear:                Int,
    incomeSourceEmployment: Seq[IncomeSource],
    previousEmployments:    Seq[IncomeSource],
    incomeSourcePension:    Seq[IncomeSource],
    nonTaxCodeIncomes:      NonTaxCodeIncome,
    taxAccountSummary:      TaxAccountSummary,
    p800Summary:            Option[P800Summary],
    employmentBenefits:     Benefits,
    taxCodeChange:          Option[TaxCodeChange]
  ): MobilePayeSummaryResponse = {

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

    val employmentPayeIncomes: Option[Seq[PayeIncome]] =
      buildPayeIncomes(incomeSourceEmployment, employment = true, Some(employmentBenefits))
    val previousEmploymentPayeIncomes: Option[Seq[PayeIncome]] =
      buildPayeIncomes(previousEmployments, employment = true)
    val pensionPayeIncomes: Option[Seq[PayeIncome]] = buildPayeIncomes(incomeSourcePension)

    val taxFreeAmount: Option[BigDecimal] = Option(taxAccountSummary.taxFreeAmount.setScale(0, RoundingMode.FLOOR))
    val estimatedTaxAmount: Option[BigDecimal] = Option(
      taxAccountSummary.totalEstimatedTax.setScale(0, RoundingMode.FLOOR)
    )
    val repayment: Option[P800Repayment] = getAndCacheP800RepaymentCheck(nino, taxYear, p800Summary)

    MobilePayeSummaryResponse(
      taxYear             = Some(taxYear),
      employments         = employmentPayeIncomes,
      previousEmployments = previousEmploymentPayeIncomes,
      repayment           = repayment,
      pensions            = pensionPayeIncomes,
      otherIncomes        = otherNonTaxCodeIncomes,
      taxCodeChange       = taxCodeChange,
      taxFreeAmount       = taxFreeAmount,
      estimatedTaxAmount  = estimatedTaxAmount
    )
  }

  def buildPayeIncomes(
    incomes:            Seq[IncomeSource],
    employment:         Boolean = false,
    employmentBenefits: Option[Benefits] = None
  ): Option[Seq[PayeIncome]] =
    incomes.map { inc =>
      PayeIncome.fromIncomeSource(inc, employment, employmentBenefits)
    } match {
      case Nil => None
      case epi => Some(epi)
    }

  private def cyPlus1InfoCheck(employments: Seq[IncomeSource]): Future[Boolean] =
    getTaxCodeLocation(employments) match {
      case Some("Welsh") => Future successful isComparisonPeriodActive(walesComparisonStartDate, walesComparisonEndDate)
      case Some("Scottish") =>
        Future successful isComparisonPeriodActive(scotlandComparisonStartDate, scotlandComparisonEndDate)
      case Some("rUK") => Future successful isComparisonPeriodActive(rUKComparisonStartDate, rUKComparisonEndDate)
      case _           => Future successful false
    }

  private def isComparisonPeriodActive(
    startDate: String,
    endDate:   String
  ): Boolean = {
    val currentTime: LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))
    if (startDate.isEmpty || endDate.isEmpty) false
    else {
      (LocalDateTime.parse(startDate).isBefore(currentTime)
      &&
      LocalDateTime.parse(endDate).isAfter(currentTime))
    }
  }

  private def getTaxCodeLocation(employments: Seq[IncomeSource]): Option[String] = {
    val latestEmployment: Option[IncomeSource] = employments.headOption
    if (latestEmployment.isDefined) {
      latestEmployment.map(emp => emp.taxCodeIncome.taxCode.charAt(0).toLower) match {
        case Some('c') => Some("Welsh")
        case Some('s') => Some("Scottish")
        case _         => Some("rUK")
      }
    } else None
  }

  def postFeedback(feedback: Feedback)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    feedbackConnector.postFeedback(feedback)

  private def getPreviousEmployments(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Seq[IncomeSource]] =
    if (previousEmploymentsEnabled)
      taiConnector
        .getMatchingTaxCodeIncomes(nino, taxYear, EmploymentIncome.toString, NotLive.toString)
    else Future successful Seq.empty

}
