import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import utils.BaseISpec

import java.io.File

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

class TestOnlyRoutesWiredISpec extends BaseISpec {

  private val applicationRouterKey = "application.router"
  private val testOnlyRoutes       = "testOnlyDoNotUseInAppConf.Routes"

  System.setProperty(applicationRouterKey, testOnlyRoutes)

  lazy val setFlagEnabledRequest: WSRequest = wsUrl("/test-only/setFlag/online-payment-integration/true")

  s"GET $setFlagEnabledRequest with $applicationRouterKey set to $testOnlyRoutes" should {
    s"Return 200 " in (await(setFlagEnabledRequest.put(Json.parse("{}"))).status shouldBe 201)
  }
}
