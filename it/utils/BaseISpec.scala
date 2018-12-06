package utils

import org.scalatestplus.play.{OneServerPerSuite, WsScalaTestClient}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import uk.gov.hmrc.mobilepaye.MobilePayeTestData
import uk.gov.hmrc.play.test.UnitSpec

trait BaseISpec extends UnitSpec with WsScalaTestClient with OneServerPerSuite with WireMockSupport with MobilePayeTestData {
  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  def config: Map[String, Any] = Map(
    "microservice.services.service-locator.enabled"           -> false,
    "auditing.enabled"                                        -> false,
    "microservice.services.service-locator.host"              -> wireMockHost,
    "microservice.services.service-locator.port"              -> wireMockPort,
    "microservice.services.tai.host"                          -> wireMockHost,
    "microservice.services.tai.port"                          -> wireMockPort,
    "auditing.consumer.baseUri.host"                          -> wireMockHost,
    "auditing.consumer.baseUri.port"                          -> wireMockPort
  )

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
