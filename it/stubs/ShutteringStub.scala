package stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.client.WireMock._

object ShutteringStub {

  def stubForShutteringDisabled: StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/mobile-paye/shuttered-status?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
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

  def stubForShutteringEnabled: StubMapping =
    stubFor(
      get(
        urlEqualTo(
          s"/mobile-shuttering/service/mobile-paye/shuttered-status?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
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
