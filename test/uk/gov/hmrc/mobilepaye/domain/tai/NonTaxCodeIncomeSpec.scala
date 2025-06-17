/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class NonTaxCodeIncomeSpec extends AnyWordSpec with Matchers {

  "UntaxedInterest" should {
    val untaxed = UntaxedInterest(UntaxedInterestIncome, BigDecimal(1234.56))

    "serialize and deserialize to/from JSON" in {
      val json = Json.toJson(untaxed)
      json mustBe Json.obj(
        "incomeComponentType" -> "UntaxedInterestIncome",
        "amount"              -> 1234.56
      )

      val parsed = json.as[UntaxedInterest]
      parsed mustBe untaxed
    }

    "format component type with spaces and upper case" in {
      untaxed.getFormattedIncomeComponentType mustBe "UNTAXED INTEREST INCOME"
    }
  }

  "OtherNonTaxCodeIncome" should {
    val other = OtherNonTaxCodeIncome(OtherIncomeEarned, BigDecimal(888.00))

    "serialize and deserialize correctly" in {
      val json = Json.toJson(other)
      json mustBe Json.obj(
        "incomeComponentType" -> "OtherIncomeEarned",
        "amount"              -> 888.00
      )

      val parsed = json.as[OtherNonTaxCodeIncome]
      parsed mustBe other
    }

    "format income type" in {
      other.getFormattedIncomeComponentType mustBe "OTHER INCOME EARNED"
    }
  }

  "NonTaxCodeIncome" should {
    val model = NonTaxCodeIncome(
      untaxedInterest        = Some(UntaxedInterest(UntaxedInterestIncome, 1000.00)),
      otherNonTaxCodeIncomes = Seq(OtherNonTaxCodeIncome(Commission, 500.00))
    )

    "serialize and deserialize fully" in {
      val json = Json.toJson(model)
      (json \ "untaxedInterest").isDefined mustBe true
      (json \ "otherNonTaxCodeIncomes").as[Seq[JsValue]].size mustBe 1

      val parsed = json.as[NonTaxCodeIncome]
      parsed mustBe model
    }

  }
}
