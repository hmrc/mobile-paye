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

import com.fasterxml.jackson.core.JsonParseException
import play.api.libs.json.{JsResultException, JsValue, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.*
import uk.gov.hmrc.mobilepaye.domain.tai.{AnnualAccount, Available, Employment, EmploymentIncome, Live, PensionIncome, TaxCodeChangeDetails, TaxCodeRecord}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import java.net.URL
import java.time.LocalDate
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
      val taiEmploymentsOnlyJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": {
             |  "employments" : [${Json.toJson(taiEmployment().copy(annualAccounts = Seq.empty))}, ${Json.toJson(
                       taiEmployment2.copy(annualAccounts = Seq.empty)
                     )}, ${Json.toJson(taiEmploymentNew3.copy(annualAccounts = Seq.empty))}]
             |}
             |}

           """.stripMargin)
      val anualAccountsjson: JsValue =
        Json.parse(s"""
               |{
               |  "data": [${Json.toJson(annualAccountsNew1)}, ${Json.toJson(annualAccountsNew2)},
               |  ${Json.toJson(annualAccountsNew3)}, ${Json.toJson(annualAccountsNew5)}]
               |
               |}
""".stripMargin)

      val incomeTaxCodeJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": [${Json.toJson(taxCodeIncomeNew1)}, ${Json.toJson(taxCodeIncomeNew2)} , ${Json.toJson(taxCodeIncome3)}]
             |
             |}
                    
           """.stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.successful(taiEmploymentsOnlyJson)
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(anualAccountsjson)
      )
      mockTaiGet(s"tax-account/$currentTaxYear/income/tax-code-incomes", Future.successful(incomeTaxCodeJson))

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe employmentIncomeSourceNew
    }

    "return an empty Seq[IncomeSource] when receiving when a NotFoundException is thrown for an authorised user" in {

      val anualAccountsjson: JsValue =
        Json.parse(s"""
             |{
             |  "data": [${Json.toJson(annualAccountsNew1)}, ${Json.toJson(annualAccountsNew2)}, ${Json.toJson(annualAccountsNew3)}]
             |
             |}

           """.stripMargin)
      val incomeTaxCodeJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": [${Json.toJson(taxCodeIncomeNew1)}, ${Json.toJson(taxCodeIncomeNew2)} ]
             |
             |}""".stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.failed(new NotFoundException("Not Found"))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(anualAccountsjson)
      )
      mockTaiGet(s"tax-account/$currentTaxYear/income/tax-code-incomes", Future.successful(incomeTaxCodeJson))

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe Seq.empty
    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.failed(new UnauthorizedException("Unauthorized"))
      )

      intercept[UnauthorizedException] {
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      }
    }

    "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.failed(new ForbiddenException("Forbidden"))
      )

      intercept[ForbiddenException] {
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      }
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.failed(new InternalServerException("Internal Server Error"))
      )

      intercept[InternalServerException] {
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
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

    "return empty response for json parse error" in {
      val taiTaxCodeChangeJson: JsValue =
        Json.parse(s"""
             |{
             |  
             |}
                    
           """.stripMargin)

      mockTaiGet(s"tax-account/tax-code-change", Future.successful(Json.toJson(taiTaxCodeChangeJson)))

      val result = await(connector.getTaxCodeChange(nino))
      result shouldBe TaxCodeChangeDetails(Seq.empty[TaxCodeRecord], Seq.empty[TaxCodeRecord])
    }

    "throw InternalServerErrorException for valid nino for authorised user when receiving a 500 response from tai" in {
      mockTaiGet(s"tax-account/tax-code-change", Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(connector.getTaxCodeChange(nino))
      }
    }
  }

  "Employments only APi - GET /employments-only/years/:taxYear " should {

    "return 200 and a valid response" in {

      val employmentOnlyJson: JsValue =
        Json.parse(s"""
             |{
             |  "data":{
             |   "employments" : [${Json.toJson(taiEmploymentOnly)}]
             |   }
             |}

           """.stripMargin)

      mockTaiGet(s"employments-only/years/2025", Future.successful(Json.toJson(employmentOnlyJson)))

      val result = await(connector.getEmploymentsOnly(nino, 2025))

      result shouldBe List(taiEmploymentOnly)

    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(s"employments-only/years/2025", Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(connector.getEmploymentsOnly(nino, 2025))
      }
    }

    "return empty List if Not found exception is thrown" in {
      mockTaiGet(s"employments-only/years/2025", Future.failed(new NotFoundException("Not Found")))
      val result = await(connector.getEmploymentsOnly(nino, 2025))
      result shouldBe Seq.empty[Employment]

    }

    "return empty List if Js result  exception is thrown" in {
      mockTaiGet(s"employments-only/years/2025", Future.failed(new JsResultException(collection.Seq.empty)))
      val result = await(connector.getEmploymentsOnly(nino, 2025))
      result shouldBe Seq.empty[Employment]

    }

  }

  "Annual Accounts APi- GET rti-payments/years/:taxYear" should {

    "return 200 and a valid response" in {

      val annualAccountsJson: JsValue =
        Json.parse(s"""
             |{
             |  "data":${Json.toJson(annualAccountsRtiSeq)}
             |
             |}
 """.stripMargin)

      mockTaiGet(s"rti-payments/years/2025", Future.successful(Json.toJson(annualAccountsJson)))

      val result = await(connector.getAnnualAccounts(nino, 2025))

      result shouldBe annualAccountsRtiSeq

    }

    "throw UnauthorisedException for valid nino but unauthorized user" in {
      mockTaiGet(s"rti-payments/years/2025", Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(connector.getAnnualAccounts(nino, 2025))
      }
    }

    "return empty List if Not found exception is thrown" in {
      mockTaiGet(s"rti-payments/years/2025", Future.failed(new NotFoundException("Not Found")))
      val result = await(connector.getAnnualAccounts(nino, 2025))
      result shouldBe Seq.empty[AnnualAccount]

    }

    "return empty List if Js result  exception is thrown" in {
      mockTaiGet(s"rti-payments/years/2025", Future.failed(new JsResultException(collection.Seq.empty)))
      val result = await(connector.getAnnualAccounts(nino, 2025))
      result shouldBe Seq.empty[AnnualAccount]

    }

  }
}
