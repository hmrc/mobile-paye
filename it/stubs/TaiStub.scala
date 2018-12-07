package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.time.TaxYear

object TaiStub {

  def personalDetailsAreFound(nino: String, person: Person): Unit = {
    stubFor(get(urlEqualTo(s"/tai/$nino/person"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "data": ${Json.toJson(person)}
             |}
          """.stripMargin)))
  }

  def taxCodeIncomesAreFound(nino: String, taxCodeIncomes: Seq[TaxCodeIncome]): Unit = {
    stubFor(get(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income/tax-code-incomes"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "data": ${Json.toJson(taxCodeIncomes)}
             |}
          """.stripMargin)))
  }

  def taxCodeIncomeNotCalled(nino: String): Unit = verify(0,
    getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income/tax-code-incomes")))

  def nonTaxCodeIncomeIsFound(nino: String, nonTaxCodeIncome: NonTaxCodeIncome): Unit = {
    stubFor(get(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "data": {
             |    "nonTaxCodeIncomes": ${Json.toJson(nonTaxCodeIncome)}
             |  }
             |}
          """.stripMargin)))
  }

  def nonTaxCodeIncomeNotCalled(nino: String): Unit = verify(0,
    getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income")))

  def employmentsAreFound(nino: String, employments: Seq[Employment]): Unit = {
    stubFor(get(urlEqualTo(s"/tai/$nino/employments/years/${TaxYear.current.currentYear}"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "data": {
             |    "employments": ${Json.toJson(employments)}
             |  }
             |}
          """.stripMargin)))
  }

  def employmentsNotCalled(nino: String): Unit = verify(0,
    getRequestedFor(urlEqualTo(s"/tai/$nino/employments/years/${TaxYear.current.currentYear}")))

  def taxAccountSummaryIsFound(nino: String, taxAccountSummary: TaxAccountSummary): Unit = {
    stubFor(get(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/summary"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(
          s"""
             |{
             |  "data": ${Json.toJson(taxAccountSummary)}
             |}
          """.stripMargin)))
  }

  def taxAccountSummaryNotCalled(nino: String): Unit = verify(0,
    getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/summary")))

}