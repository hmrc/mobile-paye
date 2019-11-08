package stubs

import java.util.Base64

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.client.WireMock._

object ShutteringStub {

  def stubForShutteringDisabled: StubMapping = {
    stubFor(
      get(urlEqualTo(s"/mobile-shuttering/service/mobile-paye/shuttered-status?journeyId=12345"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "shuttered": false,
                         |  "title":     "",
                         |  "message":    ""
                         |}
          """.stripMargin)))
  }

  def stubForShutteringEnabled: StubMapping = {
    stubFor(
      get(urlEqualTo(s"/mobile-shuttering/service/mobile-paye/shuttered-status?journeyId=12345"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "shuttered": true,
                         |  "title":     "Shuttered",
                         |  "message":   "PAYE is currently not available"
                         |}
          """.stripMargin)))
  }

  private def base64Encode(s: String): String =
    Base64.getEncoder.encodeToString(s.getBytes("UTF-8"))

}
