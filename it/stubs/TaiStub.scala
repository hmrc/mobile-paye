package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.mobilepaye.domain.IncomeSource
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.time.TaxYear

object TaiStub {

  def personalLocked(nino: String): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/person"))
        .willReturn(
          aResponse()
            .withStatus(423)
        )
    )

  def personalDetailsAreFound(
    nino:   String,
    person: Person
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/person"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(person)}
                         |}
          """.stripMargin)
        )
    )

  def stubForEmploymentIncome(
    nino:        String,
    employments: Seq[IncomeSource] = Seq.empty,
    status:      TaxCodeIncomeStatus = Live
  ): StubMapping =
    stubFor(
      get(
        urlEqualTo(s"/tai/$nino/tax-account/year/${TaxYear.current.currentYear}/income/EmploymentIncome/status/$status")
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "data": ${Json.toJson(
                         employments.map(emp => emp.copy(taxCodeIncome = emp.taxCodeIncome.copy(status = status)))
                       )}
                       |}
          """.stripMargin)
      )
    )

  def employmentsNotCalled(nino: String): Unit =
    verify(
      0,
      getRequestedFor(
        urlEqualTo(s"/tai/$nino/tax-account/year/${TaxYear.current.currentYear}/income/EmploymentIncome/status/Live")
      )
    )

  def stubForPensions(
    nino:     String,
    pensions: Seq[IncomeSource]
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/year/${TaxYear.current.currentYear}/income/PensionIncome/status/Live"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(pensions)}
                         |}
          """.stripMargin)
        )
    )

  def pensionsNotCalled(nino: String): Unit =
    verify(0,
           getRequestedFor(
             urlEqualTo(s"/tai/$nino/tax-account/year/${TaxYear.current.currentYear}/income/PensionIncome/status/Live")
           ))

  def nonTaxCodeIncomeIsFound(
    nino:             String,
    nonTaxCodeIncome: NonTaxCodeIncome
  ): StubMapping =
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
          """.stripMargin)
        )
    )

  def nonTaxCodeIncomeNotCalled(nino: String): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/income")))

  def taxAccountSummaryIsFound(
    nino:              String,
    taxAccountSummary: TaxAccountSummary,
    cyPlusone:         Boolean = false
  ): StubMapping = {
    val taxYear = if (cyPlusone) TaxYear.current.currentYear + 1 else TaxYear.current.currentYear
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/summary"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(taxAccountSummary)}
                         |}
          """.stripMargin)
        )
    )
  }

  def taxAccountSummaryNotFound(
    nino:      String,
    cyPlusone: Boolean = false
  ): StubMapping = {
    val taxYear = if (cyPlusone) TaxYear.current.currentYear + 1 else TaxYear.current.currentYear
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/summary"))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )
  }

  def taxAccountSummaryNotCalled(nino: String): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/summary")))

  def stubForBenefits(
    nino:               String,
    employmentBenefits: Benefits
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/${TaxYear.current.currentYear}/benefits"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(employmentBenefits)}
                         |}
          """.stripMargin)
        )
    )

  def stubForTaxCodeIncomes(
    nino:    String,
    taxYear: Int,
    incomes: Seq[TaxCodeIncome]
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/income/tax-code-incomes"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(incomes)}
                         |}
          """.stripMargin)
        )
    )

  def stubForEmployments(
    nino:        String,
    taxYear:     Int,
    employments: Seq[Employment]
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/employments/years/$taxYear"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": {
                         |    "employments": ${Json.toJson(employments)}
                         |  }
                         |}
          """.stripMargin)
        )
    )

  def stubForTaxCodeChangeExists(
    nino:       String,
    hasChanged: Boolean = true
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/tax-code-change/exists"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(hasChanged.toString)
        )
    )

}
