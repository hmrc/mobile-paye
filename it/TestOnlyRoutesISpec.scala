import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import utils.BaseISpec

/**
  * Need two separate tests so that the servers can be run with different system
  * property settings for the router
  */
class TestOnlyRoutesNotWiredISpec extends BaseISpec {
  private val applicationRouterKey = "application.router"

  lazy val setFlagEnabledRequest: WSRequest = wsUrl("/test-only/setFlag/online-payment-integration/true")

  System.clearProperty(applicationRouterKey)

  s"PUT $setFlagEnabledRequest  without '$applicationRouterKey' set" should {
    "Return 404" in (await(setFlagEnabledRequest.put(Json.parse("{}"))).status shouldBe 404)
  }
}
