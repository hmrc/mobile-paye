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

package uk.gov.hmrc.mobilepaye.domain.tai

import play.api.libs.json.{Format, Json}

case class UntaxedInterest(incomeComponentType: NonTaxCodeIncomeComponentType,
                           amount: BigDecimal)

object UntaxedInterest {
  implicit val format: Format[UntaxedInterest] = Json.format[UntaxedInterest]
}

case class OtherNonTaxCodeIncome(incomeComponentType: NonTaxCodeIncomeComponentType,
                                 amount: BigDecimal)

object OtherNonTaxCodeIncome {
  implicit val format: Format[OtherNonTaxCodeIncome] = Json.format[OtherNonTaxCodeIncome]
}

case class NonTaxCodeIncome(untaxedInterest: Option[UntaxedInterest], otherNonTaxCodeIncomes: Seq[OtherNonTaxCodeIncome])

object NonTaxCodeIncome {
  implicit val format: Format[NonTaxCodeIncome] = Json.format[NonTaxCodeIncome]
}
