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

import play.api.Logger
import play.api.libs.json.*

sealed trait TaxCodeIncomeStatus

case object Live extends TaxCodeIncomeStatus

case object PotentiallyCeased extends TaxCodeIncomeStatus

case object Ceased extends TaxCodeIncomeStatus

case object NotLive extends TaxCodeIncomeStatus

case class TaxCodeIncome(componentType: TaxComponentType,
                         status: TaxCodeIncomeStatus,
                         employmentId: Option[Int],
                         name: String,
                         amount: BigDecimal,
                         taxCode: String
                        )

object TaxCodeIncome {
  implicit val format: Format[TaxCodeIncome] = Json.format[TaxCodeIncome]
}

object TaxCodeIncomeStatus {

  private val logger: Logger = Logger(getClass.getName)

  def apply(value: String): TaxCodeIncomeStatus = value match {
    case "Live"              => Live
    case "NotLive"           => NotLive
    case "PotentiallyCeased" => PotentiallyCeased
    case "Ceased"            => Ceased
    case _                   => throw new IllegalArgumentException("Invalid TaxCodeIncomeStatus")
  }

  implicit val employmentStatus: Format[TaxCodeIncomeStatus] = new Format[TaxCodeIncomeStatus] {

    override def reads(json: JsValue): JsResult[TaxCodeIncomeStatus] = json.as[String] match {
      case "Live"              => JsSuccess(Live)
      case "PotentiallyCeased" => JsSuccess(PotentiallyCeased)
      case "Ceased"            => JsSuccess(Ceased)
      case "NotLive"           => JsSuccess(NotLive)
      case default => {
        logger.warn(s"Invalid Employment Status Reads -> $default")
        throw new RuntimeException("Invalid employment status reads")
      }
    }

    override def writes(taxCodeIncomeStatus: TaxCodeIncomeStatus) = JsString(taxCodeIncomeStatus.toString)
  }
}
