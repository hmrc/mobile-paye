package utils

import java.util.Base64

import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.play.WsScalaTestClient
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.mobilepaye.MobilePayeTestData

abstract class BaseISpec
    extends WordSpecLike
    with Matchers
    with WsScalaTestClient
    with GuiceOneServerPerSuite
    with WireMockSupport
    with FutureAwaits
    with DefaultAwaitTimeout
    with MobilePayeTestData {

  def shuttered: Boolean

  override implicit lazy val app: Application = appBuilder.build()

  protected val acceptJsonHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  def config: Map[String, Any] = {

    val baseConfig = Map(
      "microservice.services.service-locator.enabled" -> false,
      "auditing.enabled"                              -> false,
      "microservice.services.service-locator.port"    -> wireMockPort,
      "microservice.services.auth.port"               -> wireMockPort,
      "microservice.services.tai.port"                -> wireMockPort,
      "auditing.consumer.baseUri.port"                -> wireMockPort
    )

    if (shuttered) {
      val conf = baseConfig + ("mobilePaye.shuttering.shuttered" -> true,
      "mobilePaye.shuttering.title"   -> base64Encode("Shuttered"),
      "mobilePaye.shuttering.message" -> base64Encode("PAYE is currently not available"))
      conf
    } else {
      val conf = baseConfig + ("mobilePaye.shuttering.shuttered" -> false,
      "mobilePaye.shuttering.title"   -> base64Encode(""),
      "mobilePaye.shuttering.message" -> base64Encode(""))
      conf
    }
  }

  private def base64Encode(s: String): String =
    Base64.getEncoder.encodeToString(s.getBytes("UTF-8"))

  protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(config)

  protected implicit lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
}
