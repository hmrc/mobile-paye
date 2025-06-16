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

package uk.gov.hmrc.mobilepaye.domain.audit

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.mobilepaye.domain.OtherIncome // adjust if your package differs

class OtherIncomeAuditSpec extends AnyWordSpec with Matchers {

  "OtherIncomeAudit.fromOtherIncome" should {
    "convert OtherIncome to OtherIncomeAudit correctly" in {
      val otherIncome = OtherIncome("Dividends", BigDecimal(1234.56))
      val audit = OtherIncomeAudit.fromOtherIncome(otherIncome)

      audit.name mustBe "Dividends"
      audit.amount mustBe BigDecimal(1234.56)
    }
  }

  "OtherIncomeAudit JSON serialization" should {
    "serialize to JSON correctly" in {
      val audit = OtherIncomeAudit("Rent", BigDecimal(500.25))
      val json = Json.toJson(audit)

      (json \ "name").as[String] mustBe "Rent"
      (json \ "amount").as[BigDecimal] mustBe BigDecimal(500.25)
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "name": "Interest",
          |  "amount": 99.99
          |}
          |""".stripMargin)

      val audit = json.as[OtherIncomeAudit]
      audit mustBe OtherIncomeAudit("Interest", BigDecimal(99.99))
    }
  }
}

