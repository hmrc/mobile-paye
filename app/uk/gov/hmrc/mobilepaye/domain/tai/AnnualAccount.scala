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

import play.api.libs.json.{Format, JsError, JsNumber, JsResult, JsSuccess, JsValue, Json}
import uk.gov.hmrc.time.TaxYear

case class AnnualAccount(taxYear: TaxYear, payments: Seq[Payment], realTimeStatus: RealTimeStatus) {

  lazy val totalIncomeYearToDate: BigDecimal =
    if (payments.isEmpty) 0 else payments.max.amountYearToDate

  lazy val latestPayment: Option[Payment] = if (payments.isEmpty) None else Some(payments.max)
}

object AnnualAccount {

  implicit val formatTaxYear: Format[TaxYear] = new Format[TaxYear] {

    override def reads(j: JsValue): JsResult[TaxYear] = j match {
      case JsNumber(n) => JsSuccess(TaxYear(n.toInt))
      case x           => JsError(s"Expected JsNumber, found $x")
    }

    override def writes(v: TaxYear): JsValue = JsNumber(v.startYear)
  }

  implicit val format: Format[AnnualAccount] = Json.format[AnnualAccount]
}
