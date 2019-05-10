package stubs

import java.time.LocalDate
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Writes
import uk.gov.hmrc.mobilepaye.domain.taxcalc.{P800Status, P800Summary, RepaymentStatus}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus._

object TaxCalcStub {
  def taxCalcNoResponse(nino: String, taxYear: Int): StubMapping =
    stubFor(
      get(urlEqualTo(s"/taxcalc/$nino/taxSummary/${taxYear - 1}"))
        .willReturn(
          aResponse()
            .withStatus(404)
        )
    )

  def taxCalcValidResponse(nino: String, taxYear: Int,
                           amount: BigDecimal,
                           p800Status: P800Status,
                           paymentStatus: RepaymentStatus,
                           time: LocalDate): StubMapping = {
    def withPaidDate(): Option[LocalDate] = {
      paymentStatus match {
        case `PAYMENT_PAID` | `CHEQUE_SENT` => Option(time)
        case _ => None
      }
    }

    val p800Summary = P800Summary(p800Status, paymentStatus, amount, taxYear, withPaidDate())

    stubFor(
      get(urlEqualTo(s"/taxcalc/$nino/taxSummary/${taxYear - 1}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(implicitly[Writes[P800Summary]].writes(p800Summary).toString())
        )
    )
  }

  def taxCalcWithInstantDate(nino: String, taxYear: Int, localDate: LocalDate): StubMapping = {
    val taxCalcResponse =
      s"""{
         |      "p800_status": "Overpaid",
         |      "amount": 1000,
         |      "paymentStatus": "PAYMENT_PAID",
         |      "datePaid": "${localDate}T10:23:51Z",
         |      "taxYear": $taxYear
         |}
       """.stripMargin

    stubFor(
      get(urlEqualTo(s"/taxcalc/$nino/taxSummary/${taxYear - 1}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(taxCalcResponse)
        )
    )
  }
}
