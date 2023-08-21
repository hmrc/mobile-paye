package utils

import org.mockito.MockitoSugar.mock
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.Base64
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.mobilepaye.MobilePayeTestData
import uk.gov.hmrc.mobilepaye.services.admin.FeatureFlagService

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.Future

abstract class BaseISpec
    extends AnyWordSpecLike
    with Matchers
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout
    with MobilePayeTestData {

  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader:        (String, String) = "Accept"        -> "application/vnd.hmrc.1.0+json"
  protected val authorisationJsonHeader: (String, String) = "AUTHORIZATION" -> "Bearer 123"

  def config: Map[String, Any] =
    Map[String, Any](
      "auditing.enabled"                                    -> false,
      "microservice.services.auth.port"                     -> wireMockPort,
      "microservice.services.tai.port"                      -> wireMockPort,
      "microservice.services.taxcalc.port"                  -> wireMockPort,
      "microservice.services.mobile-simple-assessment.port" -> wireMockPort,
      "microservice.services.mobile-shuttering.port"        -> wireMockPort,
      "microservice.services.mobile-shuttering.port"        -> wireMockPort,
      "microservice.services.mobile-feedback.port"          -> wireMockPort,
      "mongodb.uri"                                         -> "mongodb://localhost:27017/test-mobile-paye",
      "incomeTaxComparisonPeriod.scotland.startDate" -> LocalDateTime
        .now(ZoneId.of("Europe/London"))
        .minusDays(10)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd\'T\'HH:mm:ss")),
      "incomeTaxComparisonPeriod.scotland.endDate" -> LocalDateTime
        .now(ZoneId.of("Europe/London"))
        .plusDays(10)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd\'T\'HH:mm:ss")),
      "p800CacheEnabled"                       -> false,
      "numberOfPreviousYearsToShowPayeSummary" -> 1
    )

  def getRequestWithAuthHeaders(url: String): Future[WSResponse] =
    wsUrl(url).addHttpHeaders(acceptJsonHeader, authorisationJsonHeader).get()

  def postRequestWithAuthHeaders(
    url:      String,
    feedback: JsValue
  ): Future[WSResponse] =
    wsUrl(url).withHttpHeaders(acceptJsonHeader, authorisationJsonHeader).post(feedback)

  private def base64Encode(s: String): String =
    Base64.getEncoder.encodeToString(s.getBytes("UTF-8"))

  val mockFeatureFlagService: FeatureFlagService =
    mock[FeatureFlagService]

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[FeatureFlagService].toInstance(mockFeatureFlagService)
      )
      .configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
