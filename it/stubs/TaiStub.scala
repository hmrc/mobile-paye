package stubs

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.mobilepaye.domain.IncomeSource
import uk.gov.hmrc.mobilepaye.domain.tai.*
import uk.gov.hmrc.time.TaxYear

object TaiStub {

  def stubForJsonErrorEmploymentOnlyIncome(
    nino: String,
    employments: Seq[IncomeSource] = Seq.empty,
    status: TaxCodeIncomeStatus = Live,
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping =
    stubFor(
      get(
        urlEqualTo(s"/tai/$nino/employments-only/years/$taxYear")
      ).willReturn(
        aResponse()
          .withStatus(0)
          .withBody(s"""
               |{
               |  
               |}

             """.stripMargin)
      )
    )

  def employmentsNotCalled(
    nino: String,
    taxYear: Int = TaxYear.current.currentYear
  ): Unit =
    verify(
      0,
      getRequestedFor(
        urlEqualTo(s"/tai/$nino/employments-only/years/$taxYear")
      )
    )

  def stubForPensions(
    nino: String,
    pensions: Seq[IncomeSource],
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/year/$taxYear/income/PensionIncome/status/Live"))
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

  def stubForAllEmploymentsOnly(
    nino: String,
    employmentOnly: Seq[Employment],
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/employments-only/years/$taxYear"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                   |{
                   |  "data": {
                   |  "employments" : ${Json.toJson(employmentOnly)}
                   |  }
                   |}

                 """.stripMargin)
        )
    )

  def annualAccountsNotCalled(
    nino: String,
    taxYear: Int = TaxYear.current.currentYear
  ): Unit =
    verify(0,
           getRequestedFor(
             urlEqualTo(s"/tai/$nino/rti-payments/years/$taxYear")
           )
          )

  def nonTaxCodeIncomeIsFound(
    nino: String,
    nonTaxCodeIncome: NonTaxCodeIncome,
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/income"))
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

  def nonTaxCodeIncomeNotCalled(
    nino: String,
    taxYear: Int = TaxYear.current.currentYear
  ): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/income")))

  def taxAccountSummaryIsFound(
    nino: String,
    taxAccountSummary: TaxAccountSummary,
    cyPlusone: Boolean = false,
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping = {
    val adjustedTaxYear = if (cyPlusone) taxYear + 1 else taxYear
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$adjustedTaxYear/summary"))
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
    nino: String,
    cyPlusone: Boolean = false,
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping = {
    val adjustedTaxYear = if (cyPlusone) taxYear + 1 else taxYear
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$adjustedTaxYear/summary"))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )
  }

  def taxAccountSummaryNotCalled(
    nino: String,
    taxYear: Int = TaxYear.current.currentYear
  ): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/summary")))

  def stubForBenefits(
    nino: String,
    employmentBenefits: Benefits,
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/benefits"))
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
    nino: String,
    taxYear: Int = TaxYear.current.currentYear,
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
  def stubForErrorJsonTaxCodeIncomes(
    nino: String,
    taxYear: Int,
    incomes: Seq[TaxCodeIncome] = Seq.empty
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/income/tax-code-incomes"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                   |{
                   |
                   |}

                 """.stripMargin)
        )
    )

  def stubForEmploymentsOnly(
    nino: String,
    employments: Seq[Employment],
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/employments-only/years/$taxYear"))
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

  def stubForAnnualAccounts(
    nino: String,
    annualAccounts: Seq[AnnualAccount],
    taxYear: Int = TaxYear.current.currentYear
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/rti-payments/years/$taxYear"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                 |{
                 |  "data": ${Json.toJson(annualAccounts)}
                 |
                 |}

               """.stripMargin)
        )
    )

  def stubForEmploymentsOnlyJsonError(
    nino: String,
    taxYear: Int,
    employments: Seq[Employment]
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/employments-only/years/$taxYear"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                   |{
                   |  
                   |}

                 """.stripMargin)
        )
    )

  def stubForTaxCodeChangeExists(
    nino: String,
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

  def stubForTaxCodes(
    nino: String,
    taxYear: Int,
    taxCodeRecords: Seq[TaxCodeRecord]
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/tax-code/latest"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(taxCodeRecords)}
                         |}
          """.stripMargin)
        )
    )

  def stubForTaxCodeChange(
    nino: String,
    taxCodeChangeDetails: TaxCodeChangeDetails
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/tai/$nino/tax-account/tax-code-change"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "data": ${Json.toJson(taxCodeChangeDetails)}
                         |}
          """.stripMargin)
        )
    )

}
