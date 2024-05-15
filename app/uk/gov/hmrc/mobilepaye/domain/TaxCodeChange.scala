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

import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class TaxCodeChange(
  hasChanged:         Boolean,
  startDate:          Option[LocalDate] = None,
  taxCodeChangeUrl:   String = "/check-income-tax/tax-code-change/tax-code-comparison",
  taxCodeChangeUrlCy: String = "/check-income-tax/tax-code-change/tax-code-comparison/cy")

object TaxCodeChange {
  implicit val formats: Format[TaxCodeChange] = Json.format[TaxCodeChange]
}
