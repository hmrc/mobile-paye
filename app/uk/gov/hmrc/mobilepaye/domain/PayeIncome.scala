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
import uk.gov.hmrc.mobilepaye.domain.tai.{Benefits, Employment, Live, Payment, TaxCodeIncomeStatus}

import java.time.LocalDate
import scala.math.BigDecimal.RoundingMode

case class PayeIncome(name: String,
                      status: TaxCodeIncomeStatus,
                      payrollNumber: Option[String] = None,
                      taxCode: String,
                      amount: BigDecimal,
                      payeNumber: String,
                      incomeDetailsLink: String,
                      yourIncomeCalculationDetailsLink: String,
                      updateIncomeLink: Option[String],
                      updateEmployerLink: Option[String],
                      payments: Option[Seq[Payment]],
                      employmentBenefits: Option[EmploymentBenefits],
                      endDate: Option[LocalDate]
                     )

object PayeIncome {

  def fromIncomeSource(
    incomeSource: IncomeSource,
    employment: Boolean,
    employmentBenefits: Option[Benefits] = None
  ): PayeIncome = {
    val empId: Int = incomeSource.taxCodeIncome.flatMap(_.employmentId).getOrElse(incomeSource.employment.sequenceNumber)
    PayeIncome(
      name                             = incomeSource.taxCodeIncome.map(_.name).getOrElse(incomeSource.employment.name),
      status                           = incomeSource.employment.employmentStatus,
      payrollNumber                    = incomeSource.employment.payrollNumber,
      taxCode                          = incomeSource.taxCodeIncome.map(_.taxCode).getOrElse(""),
      amount                           = incomeSource.taxCodeIncome.map(_.amount.setScale(0, RoundingMode.FLOOR)).getOrElse(0L),
      payeNumber                       = s"${incomeSource.employment.taxDistrictNumber}/${incomeSource.employment.payeNumber}",
      incomeDetailsLink                = getIncomeDetailsLink(incomeSource),
      updateEmployerLink               = Some(s"/check-income-tax/update-remove-employment/decision/$empId"),
      yourIncomeCalculationDetailsLink = s"/check-income-tax/your-income-calculation-details/$empId",
      updateIncomeLink =
        if (employment && incomeSource.employment.employmentStatus.equals(Live))
          Some(
            s"/check-income-tax/update-income/load/$empId"
          )
        else None,
      payments =
        if (incomeSource.employment.annualAccounts.headOption.map(_.payments).getOrElse(Seq.empty).isEmpty) None
        else incomeSource.employment.annualAccounts.headOption.map(_.payments.sorted(Payment.dateOrdering.reverse)),
      employmentBenefits = employmentBenefits.flatMap(
        buildEmploymentBenefits(_, incomeSource.taxCodeIncome.map(_.employmentId).getOrElse(Some(incomeSource.employment.sequenceNumber)))
      ),
      endDate = incomeSource.employment.endDate
    )
  }

  def fromEmployment(
    employment: Employment,
    taxCode: Option[String],
    employmentBenefits: Option[Benefits] = None,
    taxYear: Int
  ): PayeIncome =
    PayeIncome(
      name          = employment.name,
      status        = employment.employmentStatus,
      payrollNumber = employment.payrollNumber,
      taxCode       = taxCode.getOrElse(""),
      amount = employment.annualAccounts.headOption
        .flatMap(accounts => accounts.latestPayment.map(_.taxAmountYearToDate))
        .getOrElse(BigDecimal(0)),
      payeNumber                       = s"${employment.taxDistrictNumber}/${employment.payeNumber}",
      incomeDetailsLink                = s"/check-income-tax/your-income-calculation-previous-year/$taxYear/${employment.sequenceNumber}",
      yourIncomeCalculationDetailsLink = s"/check-income-tax/your-income-calculation-details/${employment.sequenceNumber}",
      updateIncomeLink                 = None,
      updateEmployerLink               = Some(s"/check-income-tax/update-remove-employment/decision/${employment.sequenceNumber}"),
      payments =
        if (employment.annualAccounts.headOption.map(_.payments).getOrElse(Seq.empty).isEmpty) None
        else employment.annualAccounts.headOption.map(_.payments.sorted(Payment.dateOrdering.reverse)),
      employmentBenefits = employmentBenefits.flatMap(
        buildEmploymentBenefits(_, Some(employment.sequenceNumber))
      ),
      endDate = employment.endDate
    )

  private def buildEmploymentBenefits(
    benefits: Benefits,
    empId: Option[Int]
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

  private def getIncomeDetailsLink(incomeSource: IncomeSource): String = {
    incomeSource.taxCodeIncome.map(_.status) match {
      case Some(status) if status.equals(Live) =>
        s"/check-income-tax/income-details/${incomeSource.taxCodeIncome.flatMap(_.employmentId).getOrElse(incomeSource.employment.sequenceNumber)}"
      case _ =>
        s"/check-income-tax/your-income-calculation-details/${incomeSource.taxCodeIncome.flatMap(_.employmentId).getOrElse(incomeSource.employment.sequenceNumber)}"
    }
  }

  implicit val format: OFormat[PayeIncome] = Json.format[PayeIncome]
}
