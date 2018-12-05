/*
 * Copyright 2018 HM Revenue & Customs
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

@Singleton
class MobilePayeService @Inject()(taiConnector: TaiConnector) {


  def getMobilePayeResponse(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MobilePayeResponse] = {


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
              amount = tci.amount,
              link = "TODO")) //TODO Implement these links
        } match {
          case Nil => None
          case epi => Some(epi)
        }
      }

      val otherIncomes: Option[Seq[OtherIncome]] = nonTaxCodeIncomes.otherNonTaxCodeIncomes.map(income => OtherIncome(
        name = income.incomeComponentType.toString.replaceAll("(\\p{Ll})(\\p{Lu})", "$1 $2").toUpperCase,
        amount = income.amount
      )) match {
        case Nil => None
        case oi => Some(oi)
      }

      val liveEmployments: Seq[TaxCodeIncome] = taxCodeIncomes.filter(emp => emp.componentType == EmploymentIncome && emp.status == Live)

      val employmentPayeIncomes: Option[Seq[PayeIncome]] = buildPayeIncomes(employments, liveEmployments)

      val livePensions: Seq[TaxCodeIncome] = taxCodeIncomes.filter(emp => emp.componentType == PensionIncome && emp.status == Live)

      val pensionPayeIncomes: Option[Seq[PayeIncome]] = buildPayeIncomes(employments, livePensions)

      MobilePayeResponse(employments = employmentPayeIncomes,
        pensions = pensionPayeIncomes,
        otherIncomes = otherIncomes,
        taxFreeAmount = taxAccountSummary.taxFreeAmount,
        estimatedTaxAmount = taxAccountSummary.totalEstimatedTax)
    }

    for {
      taxCodeIncomes <- taiConnector.getTaxCodeIncomes(nino)
      nonTaxCodeIncomes <- taiConnector.getNonTaxCodeIncome(nino)
      employments <- taiConnector.getEmployments(nino)
      taxAccountSummary <- taiConnector.getTaxAccountSummary(nino)
      mobilePayeResponse: MobilePayeResponse = buildMobilePayeResponse(taxCodeIncomes, nonTaxCodeIncomes, employments, taxAccountSummary)
    } yield mobilePayeResponse
  }

}
