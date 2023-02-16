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

package uk.gov.hmrc.mobilepaye.domain.tai

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class Benefits(
  companyCarBenefits: Seq[CompanyCarBenefit],
  otherBenefits:      Seq[GenericBenefit])

object Benefits {
  implicit val format: OFormat[Benefits] = Json.format[Benefits]
}

case class CompanyCarBenefit(
  employmentSeqNo: Int,
  grossAmount:     BigDecimal,
  companyCars:     Seq[CompanyCar],
  version:         Option[Int] = None)

object CompanyCarBenefit {
  implicit val format: OFormat[CompanyCarBenefit] = Json.format[CompanyCarBenefit]
}

case class CompanyCar(
  carSeqNo:                           Int,
  makeModel:                          String,
  hasActiveFuelBenefit:               Boolean,
  dateMadeAvailable:                  Option[LocalDate],
  dateActiveFuelBenefitMadeAvailable: Option[LocalDate],
  dateWithdrawn:                      Option[LocalDate])

object CompanyCar {
  implicit val format: OFormat[CompanyCar] = Json.format[CompanyCar]
}

case class GenericBenefit(
  benefitType:   BenefitComponentType,
  employmentId: Option[Int],
  amount:       BigDecimal)

object GenericBenefit {
  implicit val format: OFormat[GenericBenefit] = Json.format[GenericBenefit]
}
