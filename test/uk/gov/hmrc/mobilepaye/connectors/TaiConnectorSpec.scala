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

package uk.gov.hmrc.mobilepaye.connectors

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class TaiConnectorSpec extends BaseSpec {

  val mockCoreGet: CoreGet = mock[CoreGet]
  val serviceUrl: String = "tst-url"
  val connector: TaiConnector = new TaiConnector(mockCoreGet, serviceUrl)

  def mockTaiGet[T](url: String, f: Future[T]) = {
    (mockCoreGet.GET(_: String)
    (_: HttpReads[T], _: HeaderCarrier, _: ExecutionContext)).expects(
      s"$serviceUrl/tai/${nino.value}/$url", *, *, *).returning(f)
  }

  "Person - GET /tai/:nino/person" should {
    "return a valid Person when receiving a valid 200 response for an authorised user" in {
      val taiPersonJson: JsValue =
        Json.parse(
          s"""
             |{
             |  "data": ${Json.toJson(person)}
             |}
          """.stripMargin)

      mockTaiGet("person", Future.successful(taiPersonJson))

      val result = await(connector.getPerson(nino))
      result shouldBe person
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet("person", Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(connector.getPerson(nino))
      }
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      mockTaiGet("person", Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException] {
        await(connector.getPerson(nino))
      }
    }

    "throw InternalServerException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet("person", Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(connector.getPerson(nino))
      }
    }
  }

  "Tax Code Incomes - GET /tai/:nino/tax-account/:year/income/tax-code-incomes" should {
    "return a valid Seq[TaxCodeIncome] when receiving a valid 200 response for an authorised user" in {
      val taiTaxCodeIncomesJson: JsValue =
        Json.parse(
          s"""
             |{
             |  "data": ${Json.toJson(taxCodeIncomes)}
             |}
          """.stripMargin)

      mockTaiGet(s"tax-account/$currentTaxYear/income/tax-code-incomes", Future.successful(taiTaxCodeIncomesJson))

      val result = await(connector.getTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe taxCodeIncomes
    }

    "return an empty Seq[TaxCodeIncome] when a NotFoundException is thrown for an authorised user" in {
      mockTaiGet(s"tax-account/$currentTaxYear/income/tax-code-incomes", Future.failed(new NotFoundException("Not Found")))

      val result = await(connector.getTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe emptyTaxCodeIncomes
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(s"tax-account/$currentTaxYear/income/tax-code-incomes", Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(connector.getTaxCodeIncomes(nino, currentTaxYear))
      }
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      mockTaiGet(s"tax-account/$currentTaxYear/income/tax-code-incomes", Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException] {
        await(connector.getTaxCodeIncomes(nino, currentTaxYear))
      }
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(s"tax-account/$currentTaxYear/income/tax-code-incomes", Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(connector.getTaxCodeIncomes(nino, currentTaxYear))
      }
    }
  }

  "Non Tax Code Incomes - GET /tai/:nino/tax-account/:year/income" should {
    "return a valid NonTaxCodeIncome when receiving a valid 200 response for an authorised user" in {
      val taiNonTaxCodeIncomeJson: JsValue =
        Json.parse(
          s"""
             |{
             |  "data": {
             |    "nonTaxCodeIncomes": ${Json.toJson(nonTaxCodeIncomeWithoutUntaxedInterest)}
             |  }
             |}
          """.stripMargin)

      mockTaiGet(s"tax-account/$currentTaxYear/income", Future.successful(taiNonTaxCodeIncomeJson))

      val result = await(connector.getNonTaxCodeIncome(nino, currentTaxYear))
      result shouldBe nonTaxCodeIncomeWithoutUntaxedInterest
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(s"tax-account/$currentTaxYear/income", Future.failed(new UnauthorizedException("Unauthorised")))

      intercept[UnauthorizedException] {
        await(connector.getNonTaxCodeIncome(nino, currentTaxYear))
      }
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      mockTaiGet(s"tax-account/$currentTaxYear/income", Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException] {
        await(connector.getNonTaxCodeIncome(nino, currentTaxYear))
      }
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(s"tax-account/$currentTaxYear/income", Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(connector.getNonTaxCodeIncome(nino, currentTaxYear))
      }
    }
  }

  "Employments - GET /tai/:nino/employments/years/:year" should {
    "return a valid Seq[Employment] when receiving a valid 200 response for an authorised user" in {
      val taiEmploymentsJson: JsValue =
        Json.parse(
          s"""
             |{
             |  "data": {
             |    "employments": ${Json.toJson(taiEmployments)}
             |  }
             |}
          """.stripMargin)

      mockTaiGet(s"employments/years/$currentTaxYear", Future.successful(taiEmploymentsJson))

      val result = await(connector.getEmployments(nino, currentTaxYear))
      result shouldBe taiEmployments
    }

    "return an empty Seq[Employment] when receiving when a NotFoundException is thrown for an authorised user" in {
      mockTaiGet(s"employments/years/$currentTaxYear", Future.failed(new NotFoundException("Not Found")))

      val result = await(connector.getEmployments(nino, currentTaxYear))
      result shouldBe emptyEmployments
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(s"employments/years/$currentTaxYear", Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(connector.getEmployments(nino, currentTaxYear))
      }
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      mockTaiGet(s"employments/years/$currentTaxYear", Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException] {
        await(connector.getEmployments(nino, currentTaxYear))
      }
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(s"employments/years/$currentTaxYear", Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(connector.getEmployments(nino, currentTaxYear))
      }
    }
  }

  "Tax Account Summary - GET /tai/:nino/tax-account/:year/summary" should {
    "return a valid TaxAccountSummary when receiving a valid 200 response for an authorised user" in {
      val taiTaxAccountSummaryJson: JsValue =
        Json.parse(
          s"""
             |{
             |  "data": ${Json.toJson(taxAccountSummary)}
             |}
          """.stripMargin)

      mockTaiGet(s"tax-account/$currentTaxYear/summary", Future.successful(taiTaxAccountSummaryJson))

      val result = await(connector.getTaxAccountSummary(nino, currentTaxYear))
      result shouldBe taxAccountSummary
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(s"tax-account/$currentTaxYear/summary", Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(connector.getTaxAccountSummary(nino, currentTaxYear))
      }
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      mockTaiGet(s"tax-account/$currentTaxYear/summary", Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException] {
        await(connector.getTaxAccountSummary(nino, currentTaxYear))
      }
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(s"tax-account/$currentTaxYear/summary", Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(connector.getTaxAccountSummary(nino, currentTaxYear))
      }
    }
  }
}