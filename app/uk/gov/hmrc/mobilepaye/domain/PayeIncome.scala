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

package uk.gov.hmrc.mobilepaye.domain

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mobilepaye.domain.tai.{Benefits, GenericBenefit, Payment, TaxCodeIncomeStatus}

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode

case class PayeIncome(
  name:               String,
  status:             TaxCodeIncomeStatus,
  payrollNumber:      Option[String] = None,
  taxCode:            String,
  amount:             BigDecimal,
  payeNumber:         String,
  link:               String,
  updateIncomeLink:   Option[String],
  latestPayment:      Option[LatestPayment],
  payments:           Option[Seq[Payment]],
  employmentBenefits: Option[EmploymentBenefits])

object PayeIncome {

  def fromIncomeSource(
    incomeSource:       IncomeSource,
    employment:         Boolean,
    employmentBenefits: Option[Benefits] = None
  ): PayeIncome =
    PayeIncome(
      name          = incomeSource.taxCodeIncome.name,
      status        = incomeSource.taxCodeIncome.status,
      payrollNumber = incomeSource.employment.payrollNumber,
      taxCode       = incomeSource.taxCodeIncome.taxCode,
      amount        = incomeSource.taxCodeIncome.amount.setScale(0, RoundingMode.FLOOR),
      payeNumber    = incomeSource.employment.payeNumber,
      link =
        s"/check-income-tax/income-details/${incomeSource.taxCodeIncome.employmentId.getOrElse(throw new Exception("Employment ID not found"))}",
      updateIncomeLink = if (employment)
        Option(
          s"/check-income-tax/update-income/load/${incomeSource.taxCodeIncome.employmentId.getOrElse(throw new Exception("Employment ID not found"))}"
        )
      else None,
      latestPayment =
        if (employment)
          buildLatestPayment(
            incomeSource.employment.annualAccounts.headOption.flatMap(accounts => accounts.latestPayment),
            incomeSource.taxCodeIncome.employmentId
          )
        else None,
      if (incomeSource.employment.annualAccounts.headOption.map(_.payments).getOrElse(Seq.empty).isEmpty) None
      else incomeSource.employment.annualAccounts.headOption.map(_.payments.sorted(Payment.dateOrdering.reverse)),
      employmentBenefits.flatMap(
        buildEmploymentBenefits(_, incomeSource.taxCodeIncome.employmentId)
      )
    )

  private def buildLatestPayment(
    payment: Option[Payment],
    empId:   Option[Int]
  ): Option[LatestPayment] =
    payment.flatMap(latestPayment =>
      if (latestPayment.date.isAfter(LocalDate.now.minusDays(62))) {
        Some(
          LatestPayment(
            latestPayment.date,
            latestPayment.amount,
            latestPayment.taxAmount,
            latestPayment.nationalInsuranceAmount,
            latestPayment.amountYearToDate,
            latestPayment.taxAmountYearToDate,
            latestPayment.nationalInsuranceAmountYearToDate,
            s"/check-income-tax/your-income-calculation-details/${empId.getOrElse(throw new Exception("Employment ID not found"))}",
            futurePayment = if (latestPayment.date.isAfter(LocalDate.now())) true else false
          )
        )
      } else {
        None
      }
    )

  private def buildEmploymentBenefits(
    benefits: Benefits,
    empId:    Option[Int]
  ): Option[EmploymentBenefits] = {
    val carBenefit =
      empId.map(empId => benefits.companyCarBenefits.filter(_.employmentSeqNo == empId)).getOrElse(Seq.empty)
    val employerBenefits = empId.map(empId => benefits.otherBenefits.filter(_.employmentId.contains(empId)))
    val medicalInsurance =
      employerBenefits.getOrElse(Seq.empty).filter(_.benefitType.toString == MedicalInsurance.toString)
    val otherBenefits =
      employerBenefits.getOrElse(Seq.empty).filter(_.benefitType.toString != MedicalInsurance.toString)
    val employmentBenefits = EmploymentBenefits(
      Seq(
        Benefit(CarBenefit, carBenefit.map(_.grossAmount).sum),
        Benefit(MedicalInsurance, medicalInsurance.map(_.amount).sum),
        Benefit(OtherBenefits, otherBenefits.map(_.amount).sum)
      ).filter(_.amount > 0)
    )

    if (employmentBenefits.benefits.isEmpty) None else Some(employmentBenefits)

  }

  implicit val format: OFormat[PayeIncome] = Json.format[PayeIncome]
}
