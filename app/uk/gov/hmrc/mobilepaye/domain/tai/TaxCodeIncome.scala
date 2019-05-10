/*
 * Copyright 2019 HM Revenue & Customs
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

import enumeratum._
import play.api.libs.json._

import scala.collection.immutable

sealed trait TaxCodeIncomeStatus extends EnumEntry

object TaxCodeIncomeStatus extends Enum[TaxCodeIncomeStatus] with PlayJsonEnum[TaxCodeIncomeStatus] {
  case object Live              extends TaxCodeIncomeStatus
  case object PotentiallyCeased extends TaxCodeIncomeStatus
  case object Ceased            extends TaxCodeIncomeStatus

  override def values: immutable.IndexedSeq[TaxCodeIncomeStatus] = findValues
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
