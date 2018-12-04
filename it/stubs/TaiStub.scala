package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.time.TaxYear

object TaiStub {

  def personalDetailsAreFound(nino: String, person: Person): Unit = {
    stubFor(post(urlEqualTo(s"/tai/$nino/person"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.toJson(person).toString())))
  }

  def taxCodeIncomesAreFound(nino: String, taxCodeIncomes: Seq[TaxCodeIncome]): Unit = {
    stubFor(post(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income/tax-code-incomes"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.toJson(taxCodeIncomes).toString())))
  }

  def taxCodeIncomeNotCalled(nino: String): Unit = verify(0,
    getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income/tax-code-incomes")))

  def nonTaxCodeIncomeIsFound(nino: String, nonTaxCodeIncome: NonTaxCodeIncome): Unit = {
    stubFor(post(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.toJson(nonTaxCodeIncome).toString())))
  }

  def nonTaxCodeIncomeNotCalled(nino: String): Unit = verify(0,
    getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income")))

  def employmentsAreFound(nino: String, employments: Seq[Employment]): Unit = {
    stubFor(post(urlEqualTo(s"/tai/$nino/employments/years/${TaxYear.current.currentYear}"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.toJson(employments).toString())))
  }

  def employmentsNotCalled(nino: String): Unit = verify(0,
    getRequestedFor(urlEqualTo(s"/tai/$nino/employments/years/${TaxYear.current.currentYear}")))

  def taxAccountSummaryIsFound(nino: String, taxAccountSummary: TaxAccountSummary): Unit = {
    stubFor(post(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/summary"))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(Json.toJson(taxAccountSummary).toString())))
  }

  def taxAccountSummaryNotCalled(nino: String): Unit = verify(0,
    getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/summary")))

}
