package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.mobilepaye.domain.IncomeSource
import uk.gov.hmrc.mobilepaye.domain.citizendetails.Person
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.time.TaxYear

object TaiStub {

  def stubForEmploymentIncome(
    nino:        String,
    employments: Seq[IncomeSource] = Seq.empty,
    status:      TaxCodeIncomeStatus = Live,
    taxYear:     Int = TaxYear.current.currentYear
  ): StubMapping =
    stubFor(
      get(
        urlEqualTo(s"/tai/$nino/tax-account/year/$taxYear/income/EmploymentIncome/status/$status")
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

  def employmentsNotCalled(
    nino:    String,
    taxYear: Int = TaxYear.current.currentYear
  ): Unit =
    verify(
      0,
      getRequestedFor(
        urlEqualTo(s"/tai/$nino/tax-account/year/$taxYear/income/EmploymentIncome/status/Live")
      )
    )

  def stubForPensions(
    nino:     String,
    pensions: Seq[IncomeSource],
    taxYear:  Int = TaxYear.current.currentYear
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

  def pensionsNotCalled(
    nino:    String,
    taxYear: Int = TaxYear.current.currentYear
  ): Unit =
    verify(0,
           getRequestedFor(
             urlEqualTo(s"/tai/$nino/tax-account/year/$taxYear/income/PensionIncome/status/Live")
           ))

  def nonTaxCodeIncomeIsFound(
    nino:             String,
    nonTaxCodeIncome: NonTaxCodeIncome,
    taxYear:          Int = TaxYear.current.currentYear
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
    nino:    String,
    taxYear: Int = TaxYear.current.currentYear
  ): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/income")))

  def taxAccountSummaryIsFound(
    nino:              String,
    taxAccountSummary: TaxAccountSummary,
    cyPlusone:         Boolean = false,
    taxYear:           Int = TaxYear.current.currentYear
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
    nino:      String,
    cyPlusone: Boolean = false,
    taxYear:   Int = TaxYear.current.currentYear
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
    nino:    String,
    taxYear: Int = TaxYear.current.currentYear
  ): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/tai/$nino/tax-account/$taxYear/summary")))

  def stubForBenefits(
    nino:               String,
    employmentBenefits: Benefits,
    taxYear:            Int = TaxYear.current.currentYear
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

  def stubForTaxCodes(
    nino:           String,
    taxYear:        Int,
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
    nino:                 String,
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
