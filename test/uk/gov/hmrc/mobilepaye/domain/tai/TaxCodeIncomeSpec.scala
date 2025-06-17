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

class TaxCodeIncomeSpec extends AnyWordSpec with Matchers {

  "TaxCodeIncomeStatus" should {

    "serialize and deserialize all valid values" in {
      val statuses: Seq[TaxCodeIncomeStatus] = Seq(Live, Ceased, PotentiallyCeased, NotLive)

      statuses.foreach { status =>
        val json = Json.toJson(status)
        Json.fromJson[TaxCodeIncomeStatus](json).get mustBe status
      }
    }

    "fail on invalid value during deserialization" in {
      val invalidJson = JsString("Retired")

      val ex = intercept[RuntimeException] {
        Json.fromJson[TaxCodeIncomeStatus](invalidJson).get
      }
      ex.getMessage must include("Invalid employment status reads")
    }
  }

  "TaxCodeIncome" should {
    val income = TaxCodeIncome(
      componentType = EmploymentIncome,
      status        = Live,
      employmentId  = Some(123),
      name          = "ACME Corp",
      amount        = BigDecimal(50000.55),
      taxCode       = "1250L"
    )

    "serialize to JSON" in {
      val json = Json.toJson(income)
      (json \ "name").as[String] mustBe "ACME Corp"
      (json \ "status").as[String] mustBe "Live"
      (json \ "amount").as[BigDecimal] mustBe BigDecimal(50000.55)
    }

    "deserialize from valid JSON" in {
      val jsonStr =
        s"""
           |{
           |  "componentType": "EmploymentIncome",
           |  "status": "Live",
           |  "employmentId": 123,
           |  "name": "ACME Corp",
           |  "amount": 50000.55,
           |  "taxCode": "1250L"
           |}
           |""".stripMargin

      val parsed = Json.parse(jsonStr).as[TaxCodeIncome]
      parsed mustBe income
    }
  }
}
