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

import cats.implicits.*
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.connectors.TaiConnector
import uk.gov.hmrc.mobilepaye.domain.{HistoricTaxCodeIncome, IncomeTaxYear}
import uk.gov.hmrc.mobilepaye.domain.tai.{Employment, Payment, PensionIncome, TaxCodeIncome}
import uk.gov.hmrc.mongo.play.json.Codecs.logger
import uk.gov.hmrc.time.TaxYear

import java.time.{LocalDate, ZoneId}
import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeTaxHistoryService @Inject() (taiConnector: TaiConnector,
                                         @Named("numberOfPreviousYearsToShowIncomeTaxHistory") IncomeTaxHistoryYears: Int
                                        ) {

  def getIncomeTaxHistoryYearsList(
    nino: Nino
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[IncomeTaxYear]] = {
    val currentTaxYear: TaxYear = getCurrentTaxYear
    val taxYears: List[TaxYear] =
      Range(currentTaxYear.startYear, currentTaxYear.startYear - IncomeTaxHistoryYears, -1)
        .map(TaxYear(_))
        .toList

    taxYears traverse (taxYear => {
      getIncomeTaxYear(nino, taxYear).recover { case e: Exception =>
        logger.info(s"Couldn't get taxYear info for $taxYear due to: \n$e")
        IncomeTaxYear(taxYear, None)
      }
    })

  }

  private def getCurrentTaxYear: TaxYear = {
    val currentDate = LocalDate.now(ZoneId.of("Europe/London"))
    val taxYear = TaxYear(currentDate.getYear)
    if (currentDate isBefore taxYear.starts) {
      taxYear.previous
    } else {
      taxYear
    }
  }

  private def getIncomeTaxYear(
    nino: Nino,
    taxYear: TaxYear
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[IncomeTaxYear] =
    for {
      maybeTaxCodeIncomeDetails <- taiConnector.getTaxCodeIncomes(nino, taxYear.startYear)
      employmentDetails         <- taiConnector.getEmployments(nino, taxYear.startYear)
    } yield {
      val taxCodesMap = maybeTaxCodeIncomeDetails.groupBy(_.employmentId)
      val incomeTaxHistory = employmentDetails.map { employment =>
        val maybeTaxCode = for {
          incomes                      <- taxCodesMap.get(Some(employment.sequenceNumber))
          taxCodeIncome: TaxCodeIncome <- incomes.headOption
        } yield taxCodeIncome
        val startDate: Option[LocalDate] = employment.startDate.filter(_.getYear > 1949)
        val maybeLastPayment: Option[Payment] = fetchLastPayment(employment, taxYear)
        val isPension = maybeTaxCode.exists(_.componentType == PensionIncome)

        HistoricTaxCodeIncome(
          name          = employment.name,
          payrollNumber = employment.payrollNumber.getOrElse(s"${employment.taxDistrictNumber}/${employment.payeNumber}"),
          payeReference = s"${employment.taxDistrictNumber}/${employment.payeNumber}",
          startDate     = startDate,
          endDate       = employment.endDate,
          amount        = maybeLastPayment.map(_.amountYearToDate),
          taxAmount     = maybeLastPayment.map(_.taxAmountYearToDate),
          taxCode       = maybeTaxCode.map(_.taxCode),
          isPension     = isPension
        )
      }.toList
      IncomeTaxYear(taxYear, if (incomeTaxHistory.isEmpty) None else Some(incomeTaxHistory))
    }

  private def fetchLastPayment(
    employment: Employment,
    taxYear: TaxYear
  ) = employment.annualAccounts.find(_.taxYear.startYear == taxYear.startYear).flatMap(_.payments.lastOption)

}
