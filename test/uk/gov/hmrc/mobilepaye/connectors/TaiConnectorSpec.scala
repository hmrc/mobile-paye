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

package uk.gov.hmrc.mobilepaye.connectors

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.mobilepaye.domain.tai.{EmploymentIncome, Live, PensionIncome}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class TaiConnectorSpec extends BaseSpec {
  override val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  override val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val serviceUrl: String = "https://tst-url"
  val connector: TaiConnector = new TaiConnector(mockHttpClient, serviceUrl)

  def mockTaiGet[T](
    url1: String,
    f: Future[T]
  ) = {

    val urlNew = s"$serviceUrl/tai/${nino.value}/$url1"
    (mockHttpClient
      .get(_: URL)(_: HeaderCarrier))
      .expects(url"$urlNew", *)
      .returning(mockRequestBuilder)

    (mockRequestBuilder
      .execute[T](using _: HttpReads[T], _: ExecutionContext))
      .expects(*, *)
      .returns(f)
  }

  "Non Tax Code Incomes - GET /tai/:nino/tax-account/:year/income" should {
    "return a valid NonTaxCodeIncome when receiving a valid 200 response for an authorised user" in {
      val taiNonTaxCodeIncomeJson: JsValue =
        Json.parse(s"""
                      |{
                      |  "data": {
                      |    "nonTaxCodeIncomes": ${Json.toJson(nonTaxCodeIncome)}
                      |  }
                      |}
          """.stripMargin)

      mockTaiGet(s"tax-account/$currentTaxYear/income", Future.successful(taiNonTaxCodeIncomeJson))

      val result = await(connector.getNonTaxCodeIncome(nino, currentTaxYear))
      result shouldBe nonTaxCodeIncome
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

  "Matching Tax Code Employments - GET /tai/tax-account/year/:taxYear/income/:incomeType/status/:status" should {
    "return a valid Seq[IncomeSource] when receiving a valid 200 response for an authorised user for Employment" in {
      val taiEmploymentsJson: JsValue =
        Json.parse(s"""
                      |{
                      |  "data": [{
                      |    "taxCodeIncome": ${Json.toJson(taxCodeIncome)},
                      |    "employment": ${Json.toJson(taiEmployment())}
                      |  },
                      |  {
                      |    "taxCodeIncome": ${Json.toJson(taxCodeIncome2)},
                      |    "employment": ${Json.toJson(taiEmployment2)}
                      |  }
                      |   ]
                      |}
          """.stripMargin)

      mockTaiGet(s"tax-account/year/$currentTaxYear/income/${EmploymentIncome.toString}/status/${Live.toString}",
                 Future.successful(taiEmploymentsJson)
                )

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear, EmploymentIncome.toString, Live.toString))
      result shouldBe employmentIncomeSource
    }

    "return a valid Seq[IncomeSource] when receiving a valid 200 response for an authorised user for Pensions" in {
      val taiJson: JsValue =
        Json.parse(s"""
                      |{
                      |  "data": [{
                      |    "taxCodeIncome": ${Json.toJson(taxCodeIncome3)},
                      |    "employment": ${Json.toJson(taiEmployment3)}
                      |  }
                      | ]
                      |}
          """.stripMargin)

      mockTaiGet(s"tax-account/year/$currentTaxYear/income/${PensionIncome.toString}/status/${Live.toString}", Future.successful(taiJson))

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear, PensionIncome.toString, Live.toString))
      result shouldBe pensionIncomeSource
    }

    "return an empty Seq[IncomeSource] when receiving when a NotFoundException is thrown for an authorised user" in {
      mockTaiGet(s"tax-account/year/$currentTaxYear/income/${EmploymentIncome.toString}/status/${Live.toString}",
                 Future.failed(new NotFoundException("Not Found"))
                )

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear, EmploymentIncome.toString, Live.toString))
      result shouldBe Seq.empty
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(
        s"tax-account/year/$currentTaxYear/income/${EmploymentIncome.toString}/status/${Live.toString}",
        Future.failed(new UnauthorizedException("Unauthorized"))
      )

      intercept[UnauthorizedException] {
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear, EmploymentIncome.toString, Live.toString))
      }
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      mockTaiGet(s"tax-account/year/$currentTaxYear/income/${EmploymentIncome.toString}/status/${Live.toString}",
                 Future.failed(new ForbiddenException("Forbidden"))
                )

      intercept[ForbiddenException] {
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear, EmploymentIncome.toString, Live.toString))
      }
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(
        s"tax-account/year/$currentTaxYear/income/${EmploymentIncome.toString}/status/${Live.toString}",
        Future.failed(new InternalServerException("Internal Server Error"))
      )

      intercept[InternalServerException] {
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear, EmploymentIncome.toString, Live.toString))
      }
    }
  }

  "Tax Account Summary - GET /tai/:nino/tax-account/:year/summary" should {
    "return a valid TaxAccountSummary when receiving a valid 200 response for an authorised user" in {
      val taiTaxAccountSummaryJson: JsValue =
        Json.parse(s"""
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

  "Tax Code Change Exists - GET /tai/:nino/tax-account/tax-code-change/exists" should {
    "return a valid response when receiving a valid 200 response for an authorised user" in {

      mockTaiGet(s"tax-account/tax-code-change/exists", Future.successful(Json.toJson(false)))

      val result = await(connector.getTaxCodeChangeExists(nino))
      result shouldBe false
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(s"tax-account/tax-code-change/exists", Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(connector.getTaxCodeChangeExists(nino))
      }
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(s"tax-account/tax-code-change/exists", Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(connector.getTaxCodeChangeExists(nino))
      }
    }
  }

  "Tax Code Change - GET /tai/:nino/tax-account/tax-code-change" should {
    "return a valid response when receiving a valid 200 response for an authorised user" in {

      val taiTaxCodeChangeJson: JsValue =
        Json.parse(s"""
                      |{
                      |  "data": ${Json.toJson(taxCodeChangeDetails)}
                      |}
          """.stripMargin)

      mockTaiGet(s"tax-account/tax-code-change", Future.successful(Json.toJson(taiTaxCodeChangeJson)))

      val result = await(connector.getTaxCodeChange(nino))
      result shouldBe taxCodeChangeDetails
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(s"tax-account/tax-code-change", Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(connector.getTaxCodeChange(nino))
      }
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(s"tax-account/tax-code-change", Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(connector.getTaxCodeChange(nino))
      }
    }
  }
}
