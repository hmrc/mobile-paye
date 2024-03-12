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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.mobilepaye.connectors.{FeedbackConnector, MobileSimpleAssessmentConnector, ShutteringConnector, TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.MobileSimpleAssessmentResponse
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status.{NotSupported, Overpaid, Underpaid}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Summary, TaxYearReconciliation}
import uk.gov.hmrc.mobilepaye.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilepaye.domain.{Feedback, IncomeSource, MobilePayeSummaryResponse, OtherIncome, P800Cache, P800Repayment, PayeIncome, TaxCodeChange}
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo

import java.time.{LocalDateTime, ZoneId}
import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

@Singleton
class MobilePayeService @Inject() (
  taiConnector:                                                    TaiConnector,
  taxCalcConnector:                                                TaxCalcConnector,
  p800CacheMongo:                                                  P800CacheMongo,
  feedbackConnector:                                               FeedbackConnector,
  mobileSimpleAssessmentConnector:                                 MobileSimpleAssessmentConnector,
  shutteringConnector:                                             ShutteringConnector,
  @Named("rUK.startDate") rUKComparisonStartDate:                  String,
  @Named("rUK.endDate") rUKComparisonEndDate:                      String,
  @Named("wales.startDate") walesComparisonStartDate:              String,
  @Named("wales.endDate") walesComparisonEndDate:                  String,
  @Named("scotland.startDate") scotlandComparisonStartDate:        String,
  @Named("scotland.endDate") scotlandComparisonEndDate:            String,
  @Named("p800CacheEnabled") p800CacheEnabled:                     Boolean,
  @Named("taxCodeChangeEnabled") taxCodeChangeEnabled:             Boolean,
  @Named("previousEmploymentsEnabled") previousEmploymentsEnabled: Boolean) {

  private val NpsTaxAccountNoEmploymentsCurrentYear = "no employments recorded for current tax year"
  private val NpsTaxAccountDataAbsentMsg            = "cannot complete a coding calculation without a primary employment"
  private val NpsTaxAccountNoEmploymentsRecorded    = "no employments recorded for this individual"
  val logger: Logger = Logger(this.getClass)

  def getMobilePayeSummaryResponse(
    nino:        Nino,
    taxYear:     Int,
    journeyId:   JourneyId
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
      reconciliations          <- getTaxYearReconciliation(nino)
      tcComparisonPeriodActive <- cyPlus1InfoCheck(taxCodeIncomesEmployment)
      cy1InfoAvailable <- if (tcComparisonPeriodActive) taiConnector.getCYPlusOneAccountSummary(nino, taxYear)
                         else Future successful false
      employmentBenefits <- taiConnector.getBenefits(nino, taxYear)
      taxCodeChangeExists <- if (taxCodeChangeEnabled) taiConnector.getTaxCodeChangeExists(nino)
                            else Future successful false
      simpleAssessment <- getSimpleAssessmentData(journeyId, reconciliations)
      p800Summary      <- getP800Summary(reconciliations, taxYear, journeyId)
      mobilePayeResponse: MobilePayeSummaryResponse = buildMobilePayeResponse(
        taxYear,
        taxCodeIncomesEmployment,
        previousEmployments,
        taxCodeIncomesPension,
        nonTaxCodeIncomes,
        taxAccountSummary,
        p800Summary,
        employmentBenefits,
        Some(TaxCodeChange(taxCodeChangeExists)),
        simpleAssessment
      )
    } yield {
      if (cy1InfoAvailable) mobilePayeResponse.copy(taxCodeLocation = getTaxCodeLocation(taxCodeIncomesEmployment))
      else mobilePayeResponse.copy(currentYearPlusOneLink           = None)
    }) recover {
      case ex if knownException(ex, nino) => MobilePayeSummaryResponse.empty
      case ex                             => throw ex
    }

  def getPerson(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Person] = taiConnector.getPerson(nino)

  def postFeedback(feedback: Feedback)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    feedbackConnector.postFeedback(feedback)

  def getCurrentTaxCode(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Option[String]] =
    taiConnector.getTaxCodeChange(nino).map { tcChangeDetails =>
      if (tcChangeDetails.current.size == 1) tcChangeDetails.current.headOption.map(_.taxCode) else None
    }

  private def getP800Summary(
    reconciliations: Option[List[TaxYearReconciliation]],
    taxYear:         Int,
    journeyId:       JourneyId
  )(implicit hc:     HeaderCarrier,
    ec:              ExecutionContext
  ): Future[Option[P800Summary]] = {
    val previousYear = taxYear - 1
    reconciliations match {
      case None => Future successful None
      case _ => {
        shutteringConnector
          .getShutteringStatus(journeyId, "mobile-paye-p800")
          .map(shuttered =>
            if (shuttered.shuttered) None
            else reconciliations.get.find(recon => recon.taxYear == previousYear).map(_.reconciliation)
          )
      }
    }
  }

  private def getTaxYearReconciliation(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Option[List[TaxYearReconciliation]]] =
    if (p800CacheEnabled) {
      useCacheForP800Check(nino)
    } else {
      taxCalcConnector.getTaxReconciliations(nino)
    }

  private def useCacheForP800Check(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ) =
    p800CacheMongo
      .selectByNino(nino)
      .flatMap { recordFound =>
        if (recordFound.isEmpty) {
          val taxYearRecs = taxCalcConnector.getTaxReconciliations(nino)
          taxYearRecs.map { rec =>
            if (!rec.getOrElse(List.empty).exists(_.reconciliation._type != NotSupported))
              p800CacheMongo.add(P800Cache(nino))
          }
          taxYearRecs

        } else {
          Future.successful(None)
        }
      }

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

  private def getP800Repayment(
    taxYear:     Int,
    p800Summary: Option[P800Summary]
  ): Option[P800Repayment] =
    p800Summary.flatMap(summary => P800Summary.toP800Repayment(summary, taxYear))

  private def buildMobilePayeResponse(
    taxYear:                Int,
    incomeSourceEmployment: Seq[IncomeSource],
    previousEmployments:    Seq[IncomeSource],
    incomeSourcePension:    Seq[IncomeSource],
    nonTaxCodeIncomes:      NonTaxCodeIncome,
    taxAccountSummary:      TaxAccountSummary,
    p800Summary:            Option[P800Summary],
    employmentBenefits:     Benefits,
    taxCodeChange:          Option[TaxCodeChange],
    simpleAssessment:       Option[MobileSimpleAssessmentResponse]
  ): MobilePayeSummaryResponse = {

    logger.info(s"Number of employments received from TAI for tax year $taxYear: ${incomeSourceEmployment.size}")
    logger.info(s"Number of previous employments received from TAI for tax year $taxYear: ${previousEmployments.size}")

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
    val repayment: Option[P800Repayment] =
      if (p800Summary.exists(_._type == Overpaid)) getP800Repayment(taxYear, p800Summary) else None

    logger.info(s"Number of employments sent to app for tax year $taxYear: ${employmentPayeIncomes.size}")
    logger.info(
      s"Number of previous employments sent to ap for tax year $taxYear: ${previousEmploymentPayeIncomes.size}"
    )

    MobilePayeSummaryResponse(
      taxYear             = Some(taxYear),
      employments         = employmentPayeIncomes,
      previousEmployments = previousEmploymentPayeIncomes,
      repayment           = repayment,
      pensions            = pensionPayeIncomes,
      otherIncomes        = otherNonTaxCodeIncomes,
      simpleAssessment    = simpleAssessment,
      taxCodeChange       = taxCodeChange,
      taxFreeAmount       = taxFreeAmount,
      estimatedTaxAmount  = estimatedTaxAmount
    )
  }

  private def buildPayeIncomes(
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

  private def getSimpleAssessmentData(
    journeyId:       JourneyId,
    reconciliations: Option[List[TaxYearReconciliation]]
  )(implicit hc:     HeaderCarrier
  ): Future[Option[MobileSimpleAssessmentResponse]] = {
    val underpaidRecs = reconciliations
      .map(taxYearReconList => taxYearReconList.filter(_.reconciliation._type == Underpaid))
      .getOrElse(List.empty)
    if (underpaidRecs.isEmpty) Future successful None
    else mobileSimpleAssessmentConnector.getSimpleAssessmentLiabilities(journeyId)
  }

}
