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

package uk.gov.hmrc.mobilepaye.domain.audit

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.mobilepaye.MobilePayeTestData
import uk.gov.hmrc.mobilepaye.domain.audit.{MobilePayeSummaryResponseAudit, OtherIncomeAudit, PayeIncomeAudit}

class MobilePayeSummaryResponseAuditSpec extends AnyWordSpec with Matchers with MobilePayeTestData {
  "MobilePayeSummaryResponseAudit" should {

    "serialize and deserialize MobilePayeSummaryResponseAudit" in {
      val audit = fullMobilePayeAudit

      val json = Json.toJson(audit)
      val result = json.validate[MobilePayeSummaryResponseAudit]

      result mustBe JsSuccess(audit)
    }

    "serialize and deserialize OtherIncomeAudit" in {
      val otherIncome = OtherIncomeAudit(
        name   = "Bank Interest",
        amount = BigDecimal(1250.75)
      )

      val json = Json.toJson(otherIncome)
      val result = json.validate[OtherIncomeAudit]

      result mustBe JsSuccess(otherIncome)
    }
    
    "serialize and deserialize PayeIncomeAudit" in {
      val payeIncome = PayeIncomeAudit(
        name = "TESCO",
        taxCode = "1250L",
        amount = BigDecimal(50000.55)
      )

      val json = Json.toJson(payeIncome)
      val result = json.validate[PayeIncomeAudit]

      result mustBe JsSuccess(payeIncome)
    }
  }
}
