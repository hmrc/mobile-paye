/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.mobilepaye.domain.tai.{AnnualAccount, Available, Employment, EmploymentIncome, Live, Payment, TaxCodeIncome}
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import scala.collection.immutable.Seq

class IncomeSourceSpec extends AnyWordSpec with Matchers {

  "IncomeSource" should {

    val employment = Employment(
      name             = "TESCO",
      employmentStatus = Live,
      payrollNumber    = Some("ABC123"),
      sequenceNumber   = 3,
      payeNumber       = "P12345",
      startDate        = Some(LocalDate.now().minusYears(4)),
      endDate          = None,
      annualAccounts = Seq(
        AnnualAccount(3,
                      TaxYear(LocalDate.now().getYear),
                      Seq(
                        Payment(LocalDate.now(), 100, 20, 10, 50, 5, 2)
                      ),
                      Available
                     )
      ),
      taxDistrictNumber = "123",
      employmentType    = EmploymentIncome
    )
    val incomeSource = IncomeSource(
      taxCodeIncome = Some(
        TaxCodeIncome(
          componentType = EmploymentIncome,
          status        = Live,
          employmentId  = Some(123),
          name          = "ACME Corp",
          amount        = BigDecimal(50000.55),
          taxCode       = "1250L"
        )
      ),
      employment = employment
    )
    "serialize to JSON when taxCodeIncome is defined" in {

      val json = Json.toJson(incomeSource)

      (json \ "taxCodeIncome").as[JsObject] mustBe a[JsObject]
      (json \ "employment").as[JsObject] mustBe a[JsObject]
    }

    "serialize to JSON when taxCodeIncome is None" in {
      val incomeSource = IncomeSource(
        taxCodeIncome = None,
        employment    = employment
      )

      val json = Json.toJson(incomeSource)

      (json \ "employment").as[JsObject] mustBe a[JsObject]
    }

    "deserialize JSON into IncomeSource when taxCodeIncome is defined" in {
      val json = Json.toJson(incomeSource)
      val result = json.validate[IncomeSource]

      result mustBe JsSuccess(incomeSource)

    }

    "deserialize JSON when taxCodeIncome is null" in {
      val json = Json.toJson(
        IncomeSource(
          taxCodeIncome = None,
          employment    = employment
        )
      )
      val result = json.validate[IncomeSource]

      result mustBe a[JsSuccess[_]]
      result.get.taxCodeIncome mustBe None
    }

    "round-trip serialize and deserialize IncomeSource" in {
      val incomeSource = IncomeSource(
        taxCodeIncome = None,
        employment    = employment
      )

      val json = Json.toJson(incomeSource)
      val result = json.validate[IncomeSource]

      result mustBe JsSuccess(incomeSource)
    }

    "fail deserialization when employment is missing" in {
      val json = Json.obj(
        "taxCodeIncome" -> JsNull
      )

      val result = json.validate[IncomeSource]

      result mustBe a[JsError]
    }
  }
}
