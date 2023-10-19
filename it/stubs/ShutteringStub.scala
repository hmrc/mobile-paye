package stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.client.WireMock._

object ShutteringStub {

  def stubForShutteringDisabled(service: String = "mobile-paye"): StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/$service/shuttered-status?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "shuttered": false,
                       |  "title":     "",
                       |  "message":    ""
                       |}
          """.stripMargin)
      )
    )

  def stubForShutteringEnabled(service: String = "mobile-paye"): StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/$service/shuttered-status?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
        )
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withBody(s"""
                       |{
                       |  "shuttered": true,
                       |  "title":     "Shuttered",
                       |  "message":   "PAYE is currently not available"
                       |}
          """.stripMargin)
      )
    )

}
