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

package uk.gov.hmrc.mobilepaye.connectors

import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}


class TaiConnectorSpec extends UnitSpec with WithFakeApplication {

  "Tax Code Incomes - GET /tai/:nino/tax-account/:year/income/tax-code-incomes" should {
    "return a valid Seq[TaxCodeIncome] when receiving a valid 200 response for an authorised user" in {
      pending
    }

    "return an empty Seq[TaxCodeIncome] when receiving a valid 404 response for an authorised user" in {
      pending
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      pending
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      pending
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      pending
    }
  }

  "Non Tax Code Incomes - GET /tai/:nino/tax-account/:year/income" should {
    "return a valid NonTaxCodeIncome when receiving a valid 200 response for an authorised user" in {
      pending
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      pending
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      pending
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      pending
    }
  }

  "Employments - GET /tai/:nino/employments/years/:year" should {
    "return a valid Seq[Employment] when receiving a valid 200 response for an authorised user" in {
      pending
    }

    "return an empty Seq[Employment] when receiving a valid 404 response for an authorised user" in {
      pending
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      pending
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      pending
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      pending
    }
  }

  "Tax Account Summary - GET /tai/:nino/tax-account/:year/summary" should {
    "return a valid TaxAccountSummary when receiving a valid 200 response for an authorised user" in {
      pending
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      pending
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      pending
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      pending
    }
  }
}