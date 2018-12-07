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

package uk.gov.hmrc.mobilepaye.domain

import uk.gov.hmrc.mobilepaye.utils.BaseSpec

class OtherIncomeSpec extends BaseSpec {
  "OtherIncome.apply" should {
    "Build the link when given an UNTAXED INTEREST name" in {
      val result = OtherIncome.withMaybeLink("UNTAXED INTEREST", BigDecimal(200.0))

      result.link.get shouldBe "/check-income-tax/income/bank-building-society-savings"
    }

    "Don't build a link when given anything other than UNTAXED INTEREST name" in {
      val result = OtherIncome.withMaybeLink("MAXIMUM INTEREST", BigDecimal(200.0))

      result.link.isDefined shouldBe false
    }
  }
}