package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping

object FeedbackStub {

  def postFeedbackStub(): StubMapping =
    stubFor(
      post(urlEqualTo("/mobile-feedback/feedback/mobile-paye"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |    "ableToDo": true,
               |    "howEasyScore": 5,
               |    "whyGiveScore": "It was great",
               |    "howDoYouFeelScore": 4
               |}
          """.stripMargin
          )
        )
        .willReturn(
          aResponse()
            .withStatus(204)
        )
    )
}
