package stubs

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Writes
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Status, P800Summary, RepaymentStatus, TaxYearReconciliation}

object TaxCalcStub {
  def taxCalcNoResponse(nino: String, taxYear: Int): StubMapping =
    stubFor(
      get(urlEqualTo(s"/taxcalc/$nino/reconciliations"))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )

  def taxCalcValidResponse(
    nino:          String,
    taxYear:       Int,
    amount:        BigDecimal,
    p800Status:    P800Status,
    paymentStatus: RepaymentStatus,
    time:          LocalDate): StubMapping = {
    def withPaidDate(): Option[LocalDate] =
      paymentStatus match {
        case PaymentPaid | ChequeSent => Option(time)
        case _                        => None
      }
    val taxYearReconciliation = TaxYearReconciliation(taxYear, P800Summary(p800Status, Some(paymentStatus), Some(amount), withPaidDate()))

    stubFor(
      get(urlEqualTo(s"/taxcalc/$nino/reconciliations"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(implicitly[Writes[List[TaxYearReconciliation]]].writes(List(taxYearReconciliation)).toString())
        )
    )
  }

  def taxCalcWithInstantDate(nino: String, taxYear: Int, localDate: LocalDate, yearTwoStatus: String = "payment_paid", yearTwoType: String = "overpaid"): StubMapping = {
    val taxCalcResponse =
      s"""[{"taxYear": ${taxYear - 2},"reconciliation":{
         |    "_type": "underpaid",
         |    "amount": 800,
         |    "status": "part_paid"}},
         |{"taxYear": ${taxYear - 1},"reconciliation":{
         |    "_type": "$yearTwoType",
         |    "amount": 1000,
         |    "status": "$yearTwoStatus",
         |    "datePaid": "${localDate}"}}]
       """.stripMargin

    stubFor(
      get(urlEqualTo(s"/taxcalc/$nino/reconciliations"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(taxCalcResponse)
        )
    )
  }

  def taxCalcWithNoDate(nino: String, taxYear: Int): StubMapping = {
    val taxCalcResponse =
      s"""[{"taxYear": ${taxYear - 2},"reconciliation":{
         |    "_type": "underpaid",
         |    "amount": 800,
         |    "status": "part_paid"}},
         |{"taxYear": ${taxYear - 1},"reconciliation":{
         |    "_type": "overpaid",
         |    "amount": 1000,
         |    "status": "refund"}}]
       """.stripMargin

    stubFor(
      get(urlEqualTo(s"/taxcalc/$nino/reconciliations"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(taxCalcResponse)
        )
    )
  }

  def taxCalcWithNoP800(nino: String, taxYear: Int, localDate: LocalDate): StubMapping = {
    val taxCalcResponse =
      s"""[{"taxYear": ${taxYear - 2},"reconciliation":{
         |    "_type": "balanced"}},
         |{"taxYear": ${taxYear - 1},"reconciliation":{
         |    "_type": "not_reconciled"}}]
       """.stripMargin

    stubFor(
      get(urlEqualTo(s"/taxcalc/$nino/reconciliations"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(taxCalcResponse)
        )
    )
  }

  def taxCalcNotCalled(nino: String, taxYear: Int): Unit =
    verify(0, getRequestedFor(urlEqualTo(s"/taxcalc/$nino/reconciliations")))

}
