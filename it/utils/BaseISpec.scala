package utils

import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.mobilepaye.MobilePayeTestData

trait BaseISpec
    extends WordSpecLike
    with Matchers
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout
    with MobilePayeTestData {
  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  def config: Map[String, Any] = Map(
    "microservice.services.service-locator.enabled" -> false,
    "auditing.enabled"                              -> false,
    "microservice.services.service-locator.port"    -> wireMockPort,
    "microservice.services.auth.port"               -> wireMockPort,
    "microservice.services.tai.port"                -> wireMockPort,
    "auditing.consumer.baseUri.port"                -> wireMockPort
  )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
