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

import org.scalatest.prop.TableDrivenPropertyChecks._
import play.api.libs.json.{JsResultException, JsValue, Json}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.mobilepaye.domain.tai.{AnnualAccount, Employment, PensionIncome, TaxCodeChangeDetails, TaxCodeRecord}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.mobilepaye.domain.IncomeSource

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

  private val upstreamErrorsWithoutNotFound = Table(
    "error",
    UpstreamErrorResponse("Unauthorized", 401),
    UpstreamErrorResponse("Forbidden", 403),
    UpstreamErrorResponse("Internal Server Error", 500)
  )

  private val upstreamErrors = upstreamErrorsWithoutNotFound ++ Seq(UpstreamErrorResponse("Not Found", 404))

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

      mockTaiGet(s"tax-account/$currentTaxYear/income", Future.successful(Right(taiNonTaxCodeIncomeJson)))

      val result = await(connector.getNonTaxCodeIncome(nino, currentTaxYear))
      result shouldBe nonTaxCodeIncome
    }

    "propagate UpstreamErrorResponse" in {
      forEvery(upstreamErrors) { error =>
        mockTaiGet(s"tax-account/$currentTaxYear/income", Future.successful(Left(error)))

        intercept[UpstreamErrorResponse] {
          await(connector.getNonTaxCodeIncome(nino, currentTaxYear))
        }
      }
    }
  }

  "Matching Tax Code Employments - GET employments only, accounts and tax code income" should {

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
      val annualAccountsJson: JsValue =
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
        Future.successful(Right(taiEmploymentsOnlyJson))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(Right(annualAccountsJson))
      )
      mockTaiGet(s"tax-account/$currentTaxYear/income/tax-code-incomes",
        Future.successful(Right(incomeTaxCodeJson))
      )

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe employmentIncomeSourceNewUpdated
    }

    "return a valid Seq[IncomeSource] when receiving a valid 200 response for an authorised user for Employment with same employment name for pension" in {
      val taiEmploymentsOnlyJson: JsValue =
        Json.parse(
          s"""
             |{
             |  "data": {
             |  "employments" : [${Json.toJson(taiEmployment(seqNo = 1).copy(annualAccounts = Seq.empty))}, 
             |  ${Json.toJson(
              taiEmployment(seqNo = 2).copy(annualAccounts = Seq.empty, payrollNumber = Some("ABC124"), employmentType = PensionIncome)
            )}, ${Json.toJson(taiEmployment().copy(annualAccounts = Seq.empty, payrollNumber = Some("ABC125"), employmentType = PensionIncome))}]
             |}
             |}
           """.stripMargin
        )

      val annualAccountsJson: JsValue = Json.parse(s"""{
               |  "data": []
               |}
               |""".stripMargin)

      val incomeTaxCodeJson: JsValue =
        Json.parse(s"""{ "data": [${Json.toJson(taxCodeIncomeNew1.copy(employmentId = Some(1)))},
                      | ${Json.toJson(taxCodeIncomeNew1.copy(employmentId = Some(2), taxCode = "S1250L"))} ,
                      | ${Json.toJson(taxCodeIncomeNew1.copy(employmentId = Some(3), taxCode = "S1350L"))}]
                      |
                      |}

                    """.stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.successful(Right(taiEmploymentsOnlyJson))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(Right(annualAccountsJson))
      )
      mockTaiGet(
        s"tax-account/$currentTaxYear/income/tax-code-incomes",
        Future.successful(Right(incomeTaxCodeJson))
      )

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe employmentIncomeSourceSameEmpNameUpdated
    }

    "return a valid Seq[IncomeSource] with employments even if no tax code income is present  when receiving a valid 200 response for an authorised user for Employment" in {
      val taiEmploymentsOnlyJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": {
             |  "employments" : [${Json.toJson(taiEmployment().copy(annualAccounts = Seq.empty))}, ${Json.toJson(
                       taiEmployment2.copy(annualAccounts = Seq.empty)
                     )}, ${Json.toJson(taiEmploymentNew3.copy(annualAccounts = Seq.empty))}]
             |}
             |}""".stripMargin)
      val annualAccountsJson: JsValue =
        Json.parse(s"""{
                      |  "data": [${Json.toJson(annualAccountsNew1)}, ${Json.toJson(annualAccountsNew2)},
                      |  ${Json.toJson(annualAccountsNew3)}, ${Json
                       .toJson(annualAccountsNew5)}]
                      |}
                      |""".stripMargin)

      val incomeTaxCodeJson: JsValue =
        Json.parse(s"""| { "data": []
                      |}

                    """.stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.successful(Right(taiEmploymentsOnlyJson))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(Right(annualAccountsJson))
      )
      mockTaiGet(
        s"tax-account/$currentTaxYear/income/tax-code-incomes",
        Future.successful(Right(incomeTaxCodeJson))
      )

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe employmentIncomeSourceNewUpdatedNoTaxCode
    }

    "return a valid Seq[IncomeSource] with employments even if taxcode api fails with 404 not found" in {
      val taiEmploymentsOnlyJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": {
             |  "employments" : [${Json.toJson(taiEmployment().copy(annualAccounts = Seq.empty))}, ${Json.toJson(
                       taiEmployment2.copy(annualAccounts = Seq.empty)
                     )}, ${Json.toJson(taiEmploymentNew3.copy(annualAccounts = Seq.empty))}]
             |}
             |}""".stripMargin)

      val annualAccountsJson: JsValue =
        Json.parse(s"""{
             |  "data": [${Json.toJson(annualAccountsNew1)}, ${Json.toJson(annualAccountsNew2)},
             |  ${Json.toJson(annualAccountsNew3)}, ${Json
                       .toJson(annualAccountsNew5)}]
             |}
             |""".stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.successful(Right(taiEmploymentsOnlyJson))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(Right(annualAccountsJson))
      )
      mockTaiGet(
        s"tax-account/$currentTaxYear/income/tax-code-incomes",
        Future.successful(Left(UpstreamErrorResponse(s"Tax codes not found", 404)))
      )

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe employmentIncomeSourceNewUpdatedNoTaxCode

    }

    "return a valid Seq[IncomeSource] with employments even if taxcode api fails with 500 internal server error" in {
      val taiEmploymentsOnlyJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": {
             |  "employments" : [${Json.toJson(taiEmployment().copy(annualAccounts = Seq.empty))}, ${Json.toJson(
                       taiEmployment2.copy(annualAccounts = Seq.empty)
                     )}, ${Json.toJson(taiEmploymentNew3.copy(annualAccounts = Seq.empty))}]
             |}
             |}""".stripMargin)

      val annualAccountsJson: JsValue =
        Json.parse(s"""{
             |  "data": [${Json.toJson(annualAccountsNew1)}, ${Json.toJson(annualAccountsNew2)},
             |  ${Json.toJson(annualAccountsNew3)}, ${Json
                       .toJson(annualAccountsNew5)}]
             |}
             |""".stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.successful(Right(taiEmploymentsOnlyJson))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(Right(annualAccountsJson))
      )
      mockTaiGet(
        s"tax-account/$currentTaxYear/income/tax-code-incomes",
        Future.successful(Left(UpstreamErrorResponse(s"Internal server error", 500)))
      )

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe employmentIncomeSourceNewUpdatedNoTaxCode

    }

    "return employments even if rti failed with not found exception" in {
      val taiEmploymentsOnlyJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": {
             |  "employments" : [${Json.toJson(taiEmployment().copy(annualAccounts = Seq.empty))}, ${Json.toJson(
                       taiEmployment2.copy(annualAccounts = Seq.empty)
                     )}, ${Json.toJson(taiEmploymentNew3.copy(annualAccounts = Seq.empty))}]
             |}
             |}""".stripMargin)

      val incomeTaxCodeJson: JsValue =
        Json.parse(s"""
                      |{
                      |  "data": [${Json.toJson(taxCodeIncomeNew1)}, ${Json.toJson(taxCodeIncomeNew2)} , ${Json.toJson(taxCodeIncome3)}]
                      |
                      |}

                    """.stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.successful(Right(taiEmploymentsOnlyJson))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(Left(UpstreamErrorResponse(s"No Annual accounts found", 404)))
      )
      mockTaiGet(
        s"tax-account/$currentTaxYear/income/tax-code-incomes",
        Future.successful(Right(incomeTaxCodeJson))
      )

      val result =
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      result shouldBe Seq(
        IncomeSource(Some(taxCodeIncomeNew1), taiEmployment().copy(annualAccounts = Seq.empty)),
        IncomeSource(Some(taxCodeIncomeNew2), taiEmployment2.copy(annualAccounts = Seq.empty)),
        IncomeSource(Some(taxCodeIncome3), taiEmploymentNew3.copy(annualAccounts = Seq.empty))
      )
    }

    "return not found exception if employments-only api return empty list" in {
      val taiEmploymentsOnlyJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": {
             |  "employments" : []
             |}
             |}""".stripMargin)
      val annualAccountsJson: JsValue =
        Json.parse(s"""{
             |  "data": []
             |}
             |""".stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.successful(Right(taiEmploymentsOnlyJson))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(Right(annualAccountsJson))
      )
      intercept[NotFoundException] {
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      }

    }

    "propagate UpstreamServerResponse exceptions when rti calls fail" in {
      val taiEmploymentsOnlyJson: JsValue =
        Json.parse(s"""
             |{
             |  "data": {
             |  "employments" : [${Json.toJson(taiEmployment().copy(annualAccounts = Seq.empty))}, ${Json.toJson(
                       taiEmployment2.copy(annualAccounts = Seq.empty)
                     )}, ${Json.toJson(taiEmploymentNew3.copy(annualAccounts = Seq.empty))}]
             |}
             |}""".stripMargin)

      mockTaiGet(
        s"employments-only/years/$currentTaxYear",
        Future.successful(Right(taiEmploymentsOnlyJson))
      )
      mockTaiGet(
        s"rti-payments/years/$currentTaxYear",
        Future.successful(Left(UpstreamErrorResponse(s"Internal server error", 500)))
      )

      intercept[UpstreamErrorResponse] {
        await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
      }
    }

    "propagate UpstreamServerResponse exceptions when employments-only call fails" in {

      forEvery(upstreamErrors) { error =>
        mockTaiGet(
          s"employments-only/years/$currentTaxYear",
          Future.successful(Left(error))
        )

        intercept[UpstreamErrorResponse] {
          await(connector.getMatchingTaxCodeIncomes(nino, currentTaxYear))
        }
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

      mockTaiGet(s"tax-account/$currentTaxYear/summary", Future.successful(Right(taiTaxAccountSummaryJson)))

      val result = await(connector.getTaxAccountSummary(nino, currentTaxYear))
      result shouldBe taxAccountSummary
    }

    "propagate UpstreamErrorResponse" in {

      forEvery(upstreamErrors) { error =>
        mockTaiGet(s"tax-account/$currentTaxYear/summary", Future.successful(Left(error)))

        intercept[UpstreamErrorResponse] {
          await(connector.getTaxAccountSummary(nino, currentTaxYear))
        }
      }

    }

  }

  "Tax Code Change Exists - GET /tai/:nino/tax-account/tax-code-change/exists" should {
    "return a valid response when receiving a valid 200 response for an authorised user" in {

      mockTaiGet(s"tax-account/tax-code-change/exists", Future.successful(Right(Json.toJson(false))))

      val result = await(connector.getTaxCodeChangeExists(nino))
      result shouldBe false
    }

    "return false when receiving a 404 not found response" in {
      mockTaiGet(s"tax-account/tax-code-change/exists", Future.successful(Left(UpstreamErrorResponse(s"Not found", 404))))

      val result = await(connector.getTaxCodeChangeExists(nino))
      result shouldBe false
    }

    "propagate UpstreamErrorResponse" in {
      forEvery(upstreamErrorsWithoutNotFound) { error =>
        mockTaiGet(s"tax-account/tax-code-change/exists", Future.successful(Left(error)))

        intercept[UpstreamErrorResponse] {
          await(connector.getTaxCodeChangeExists(nino))
        }
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

      mockTaiGet(s"tax-account/tax-code-change", Future.successful(Right(Json.toJson(taiTaxCodeChangeJson))))

      val result = await(connector.getTaxCodeChange(nino))
      result shouldBe taxCodeChangeDetails
    }

    "propagate UpstreamErrorResponse" in {
      forEvery(upstreamErrors) { error =>
        mockTaiGet(s"tax-account/tax-code-change", Future.successful(Left(error)))

        intercept[UpstreamErrorResponse] {
          await(connector.getTaxCodeChange(nino))
        }
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

      mockTaiGet(s"employments-only/years/2025", Future.successful(Right(Json.toJson(employmentOnlyJson))))

      val result = await(connector.getEmploymentsOnly(nino, 2025))

      result shouldBe List(taiEmploymentOnly)

    }

    "propagate UpstreamErrorResponse" in {
      forEvery(upstreamErrors) { error =>
        mockTaiGet(s"employments-only/years/2025", Future.successful(Left(error)))

        intercept[UpstreamErrorResponse] {
          await(connector.getEmploymentsOnly(nino, 2025))
        }
      }
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

      mockTaiGet(s"rti-payments/years/2025", Future.successful(Right(Json.toJson(annualAccountsJson))))

      val result = await(connector.getAnnualAccounts(nino, 2025))

      result shouldBe annualAccountsRtiSeq

    }

    "return empty List if Not found exception is thrown" in {
      mockTaiGet(s"rti-payments/years/2025", Future.successful(Left(UpstreamErrorResponse(s"Not found", 404))))
      val result = await(connector.getAnnualAccounts(nino, 2025))
      result shouldBe Seq.empty[AnnualAccount]
    }

    "propagate UpstreamErrorResponse" in {
      forEvery(upstreamErrorsWithoutNotFound) { error =>
        mockTaiGet(s"rti-payments/years/2025", Future.successful(Left(error)))

        intercept[UpstreamErrorResponse] {
          await(connector.getAnnualAccounts(nino, 2025))
        }
      }
    }

  }
}
