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

import play.api.libs.json.{Format, JsString, JsSuccess, JsValue, Json, OFormat}

case class EmploymentBenefits(
  benefits:                 Seq[Benefit],
  changeCarBenefitLink:     String = "/paye/company-car/details",
  changeMedicalBenefitLink: String = "/paye/benefits/medical-benefit",
  changeOtherBenefitLink:   String = "/digital-forms/form/tell-us-about-company-benefits/draft/guide")

object EmploymentBenefits {
  implicit val format: OFormat[EmploymentBenefits] = Json.format[EmploymentBenefits]
}

case class Benefit(
  benefitType: BenefitComponentType,
  amount:      BigDecimal)

object Benefit {
  implicit val format: OFormat[Benefit] = Json.format[Benefit]
}

object BenefitComponentType {

  implicit val formatBenefitTaxComponentType: Format[BenefitComponentType] = new Format[BenefitComponentType] {

    override def reads(json: JsValue): JsSuccess[BenefitComponentType] = json.as[String] match {
      case "MedicalInsurance" => JsSuccess(MedicalInsurance)
      case "CarBenefit"       => JsSuccess(CarBenefit)
      case "OtherBenefits"    => JsSuccess(OtherBenefits)
    }
    override def writes(benefitComponentType: BenefitComponentType) = JsString(benefitComponentType.toString)
  }
}

sealed trait BenefitComponentType
case object MedicalInsurance extends BenefitComponentType
case object CarBenefit extends BenefitComponentType
case object OtherBenefits extends BenefitComponentType
