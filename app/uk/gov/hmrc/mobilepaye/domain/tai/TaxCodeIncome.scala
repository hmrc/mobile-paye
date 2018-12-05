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

import play.api.libs.json._

sealed trait TaxCodeIncomeStatus

case object Live extends TaxCodeIncomeStatus

case object PotentiallyCeased extends TaxCodeIncomeStatus

case object Ceased extends TaxCodeIncomeStatus

object TaxCodeIncomeStatus extends TaxCodeIncomeStatus {
  implicit val formatTaxCodeIncomeSourceStatusType = new Format[TaxCodeIncomeStatus] {
    override def reads(json: JsValue): JsResult[TaxCodeIncomeStatus] = {
      json.as[String] match {
        case "Live" => JsSuccess(Live)
        case "PotentiallyCeased" => JsSuccess(PotentiallyCeased)
        case "Ceased" => JsSuccess(Ceased)
        case _ => throw new IllegalArgumentException("Invalid TaxCodeIncomeSourceStatus type")
      }
    }

    override def writes(taxCodeIncomeStatus: TaxCodeIncomeStatus) = JsString(taxCodeIncomeStatus.toString)
  }
}

case class TaxCodeIncome(componentType: TaxComponentType,
                         employmentId: Option[Int],
                         name: String,
                         amount: BigDecimal,
                         status: TaxCodeIncomeStatus,
                         taxCode: String)

object TaxCodeIncome {
  implicit val format: Format[TaxCodeIncome] = Json.format[TaxCodeIncome]
}
