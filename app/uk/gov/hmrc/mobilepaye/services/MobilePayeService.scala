/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.connectors.{TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Summary, TaxYearReconciliation}
import uk.gov.hmrc.mobilepaye.domain.{IncomeSource, MobilePayeResponse, OtherIncome, P800Cache, P800Repayment, PayeIncome}
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo

import java.time.{LocalDateTime, ZoneId}
import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

@Singleton
class MobilePayeService @Inject() (
  taiConnector:                                             TaiConnector,
  taxCalcConnector:                                         TaxCalcConnector,
  p800CacheMongo:                                           P800CacheMongo,
  @Named("uk.startDate") ukComparisonStartDate:             String,
  @Named("uk.endDate") ukComparisonEndDate:                 String,
  @Named("wales.startDate") walesComparisonStartDate:       String,
  @Named("wales.endDate") walesComparisonEndDate:           String,
  @Named("scotland.startDate") scotlandComparisonStartDate: String,
  @Named("scotland.endDate") scotlandComparisonEndDate:     String) {

  private val NpsTaxAccountNoEmploymentsCurrentYear = "no employments recorded for current tax year"
  private val NpsTaxAccountDataAbsentMsg            = "cannot complete a coding calculation without a primary employment"
  private val NpsTaxAccountNoEmploymentsRecorded    = "no employments recorded for this individual"

  def getMobilePayeResponse(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[MobilePayeResponse] = {

    def knownException(
      ex:   Throwable,
      nino: Nino
    ): Boolean = {
      val known = ex.getMessage.toLowerCase().contains(NpsTaxAccountNoEmploymentsCurrentYear) ||
        ex.getMessage.toLowerCase().contains(NpsTaxAccountDataAbsentMsg) ||
        ex.getMessage.toLowerCase().contains(NpsTaxAccountNoEmploymentsRecorded)
      Logger.info(s"[HMA-2505] - Tai exception (known: $known) for ${nino.nino} - " + ex.getMessage.toLowerCase())
      known
    }

    def getAndCacheP800RepaymentCheck(p800Summary: Option[P800Summary]): Option[P800Repayment] = {
      val repayment = p800Summary.flatMap(summary => P800Summary.toP800Repayment(summary, taxYear))
      repayment match {
        case None => {
          p800CacheMongo.add(P800Cache(nino))
          None
        }
        case _ => repayment
      }
    }

    def buildMobilePayeResponse(
      incomeSourceEmployment: Seq[IncomeSource],
      incomeSourcePension:    Seq[IncomeSource],
      nonTaxCodeIncomes:      NonTaxCodeIncome,
      taxAccountSummary:      TaxAccountSummary,
      p800Summary:            Option[P800Summary]
    ): MobilePayeResponse = {

      def buildPayeIncomes(
        incomes:          Seq[IncomeSource],
        updateIncomeLink: Boolean = false
      ): Option[Seq[PayeIncome]] =
        incomes.map { inc =>
          PayeIncome.fromIncomeSource(inc, updateIncomeLink)
        } match {
          case Nil => None
          case epi => Some(epi)
        }

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

      // $COVERAGE-OFF$
      //TODO We may need to use this in the future still but as part of HMA-546 to remediate a live issue this is unused until the underlying issue with untaxed interest is resolved.
      val untaxedInterest: Option[OtherIncome] = nonTaxCodeIncomes.untaxedInterest match {
        case Some(income) =>
          Some(
            OtherIncome.withMaybeLink(
              name   = income.getFormattedIncomeComponentType,
              amount = income.amount.setScale(0, RoundingMode.FLOOR)
            )
          )
        case None => None
      }

      val otherIncomes: Option[Seq[OtherIncome]] = (otherNonTaxCodeIncomes, untaxedInterest) match {
        case (Some(x), Some(y)) => Some(Seq(y) ++ x)
        case (Some(x), _)       => Some(x)
        case (_, Some(y))       => Some(Seq(y))
        case _                  => None
      }
      // $COVERAGE-ON$

      val employmentPayeIncomes: Option[Seq[PayeIncome]] =
        buildPayeIncomes(incomeSourceEmployment, updateIncomeLink = true)
      val pensionPayeIncomes: Option[Seq[PayeIncome]] = buildPayeIncomes(incomeSourcePension)

      val taxFreeAmount: Option[BigDecimal] = Option(taxAccountSummary.taxFreeAmount.setScale(0, RoundingMode.FLOOR))
      val estimatedTaxAmount: Option[BigDecimal] = Option(
        taxAccountSummary.totalEstimatedTax.setScale(0, RoundingMode.FLOOR)
      )
      val repayment: Option[P800Repayment] = getAndCacheP800RepaymentCheck(p800Summary)

      MobilePayeResponse(
        taxYear            = Some(taxYear),
        employments        = employmentPayeIncomes,
        repayment          = repayment,
        pensions           = pensionPayeIncomes,
        otherIncomes       = otherNonTaxCodeIncomes,
        taxFreeAmount      = taxFreeAmount,
        estimatedTaxAmount = estimatedTaxAmount
      )
    }

    (for {
      taxCodeIncomesEmployment <- taiConnector
                                   .getMatchingTaxCodeIncomes(nino, taxYear, EmploymentIncome.toString, Live.toString)
      taxCodeIncomesPension <- taiConnector
                                .getMatchingTaxCodeIncomes(nino, taxYear, PensionIncome.toString, Live.toString)
      nonTaxCodeIncomes        <- taiConnector.getNonTaxCodeIncome(nino, taxYear)
      taxAccountSummary        <- taiConnector.getTaxAccountSummary(nino, taxYear)
      reconciliations          <- getTaxYearReconciliationsForP800(nino)
      tcComparisonPeriodActive <- cyPlus1InfoCheck(taxCodeIncomesEmployment)
      cy1InfoAvailable <- if (tcComparisonPeriodActive) taiConnector.getCYPlusOneAccountSummary(nino, taxYear)
                         else Future successful false
      mobilePayeResponse: MobilePayeResponse = buildMobilePayeResponse(
        taxCodeIncomesEmployment,
        taxCodeIncomesPension,
        nonTaxCodeIncomes,
        taxAccountSummary,
        getP800Summary(reconciliations, taxYear)
      )
    } yield {
      if (cy1InfoAvailable) mobilePayeResponse.copy(taxCodeLocation = getTaxCodeLocation(taxCodeIncomesEmployment))
      else mobilePayeResponse.copy(currentYearPlusOneLink           = None)
    }) recover {
      case ex if knownException(ex, nino) => MobilePayeResponse.empty
      case ex                             => throw ex
    }
  }

  def getPerson(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Person] = taiConnector.getPerson(nino)

  def getP800Summary(
    reconciliations: Option[List[TaxYearReconciliation]],
    taxYear:         Int
  ): Option[P800Summary] = {
    val previousYear = taxYear - 1
    reconciliations match {
      case None => None
      case _    => reconciliations.get.find(recon => recon.taxYear == previousYear).map(_.reconciliation)

    }
  }

  def getTaxYearReconciliationsForP800(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ec:          ExecutionContext
  ): Future[Option[List[TaxYearReconciliation]]] = {
    val cacheCheckResult = p800CacheMongo.selectByNino(nino)
    cacheCheckResult.flatMap(recordFound =>
      if (recordFound.isEmpty) taxCalcConnector.getTaxReconciliations(nino) else Future.successful(None)
    )
  }

  private def cyPlus1InfoCheck(employments: Seq[IncomeSource]): Future[Boolean] =
    getTaxCodeLocation(employments) match {
      case Some("Welsh") => Future successful isComparisonPeriodActive(walesComparisonStartDate, walesComparisonEndDate)
      case Some("Scottish") =>
        Future successful isComparisonPeriodActive(scotlandComparisonStartDate, scotlandComparisonEndDate)
      case Some("rUK") => Future successful isComparisonPeriodActive(ukComparisonStartDate, ukComparisonEndDate)
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

}
