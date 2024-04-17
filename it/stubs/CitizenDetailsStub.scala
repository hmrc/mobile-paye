package stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.mobilepaye.domain.citizendetails.Person

object CitizenDetailsStub {

  def personalLocked(nino: String): StubMapping =
    stubFor(
      get(urlEqualTo(s"/citizen-details/$nino/designatory-details/basic"))
        .willReturn(
          aResponse()
            .withStatus(423)
        )
    )

  def personalTooManyRequests(nino: String): StubMapping =
    stubFor(
      get(urlEqualTo(s"/citizen-details/$nino/designatory-details/basic"))
        .willReturn(
          aResponse()
            .withStatus(429)
        )
    )

  def personalDetailsAreFound(
    nino:   String,
    person: Person
  ): StubMapping =
    stubFor(
      get(urlEqualTo(s"/citizen-details/$nino/designatory-details/basic"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(person).toString())
        )
    )

}
