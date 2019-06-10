package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.mobilepaye.domain.IncomeSource
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.time.TaxYear

object TaiStub {

  def personalDetailsAreFound(nino: String, person: Person): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/person"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
             |{
             |  "data": ${Json.toJson(person)}
             |}
          """.stripMargin)))

  def personalLocked(nino: String): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/person"))
        .willReturn(
          aResponse()
            .withStatus(423)
            )
    )

  def stubForEmployments(nino: String, employments: Seq[IncomeSource]): StubMapping = {
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/year/${TaxYear.current.currentYear}/income/EmploymentIncome/status/Live"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(employments)}
                         |}
          """.stripMargin)))
  }

  def employmentsNotCalled(nino: String): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/year/${TaxYear.current.currentYear}/income/EmploymentIncome/status/Live")))

  def stubForPensions(nino: String, pensions: Seq[IncomeSource]): StubMapping = {
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/year/${TaxYear.current.currentYear}/income/PensionIncome/status/Live"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(pensions)}
                         |}
          """.stripMargin)))
  }

  def pensionsNotCalled(nino: String): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/year/${TaxYear.current.currentYear}/income/PensionIncome/status/Live")))

  def taxCodeIncomeNotCalled(nino: String): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income/tax-code-incomes")))

  def nonTaxCodeIncomeIsFound(nino: String, nonTaxCodeIncome: NonTaxCodeIncome): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
             |{
             |  "data": {
             |    "nonTaxCodeIncomes": ${Json.toJson(nonTaxCodeIncome)}
             |  }
             |}
          """.stripMargin)))

  def nonTaxCodeIncomeNotCalled(nino: String): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income")))


  def taxAccountSummaryIsFound(nino: String, taxAccountSummary: TaxAccountSummary): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/summary"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
             |{
             |  "data": ${Json.toJson(taxAccountSummary)}
             |}
          """.stripMargin)))

  def taxAccountSummaryNotCalled(nino: String): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/summary")))

}
