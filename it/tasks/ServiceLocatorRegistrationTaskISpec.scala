package tasks

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, postRequestedFor, urlMatching, verify}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import play.api.libs.json.Json.toJson
import play.api.test.PlayRunners
import stubs.ServiceLocatorStub._
import uk.gov.hmrc.api.domain.Registration
import uk.gov.hmrc.mobilepaye.tasks.ServiceLocatorRegistrationTask
import utils.BaseISpec

class ServiceLocatorRegistrationTaskISpec extends BaseISpec with Eventually with ScalaFutures with PlayRunners {
  override def shuttered: Boolean = false

  def regPayloadStringFor(serviceName: String, serviceUrl: String): String =
    toJson(Registration(serviceName, serviceUrl, Some(Map("third-party-api" -> "true")))).toString

  "ServiceLocatorRegistrationTask" should {
    val task = app.injector.instanceOf[ServiceLocatorRegistrationTask]

    "register with the api platform" in {
      registrationWillSucceed()
      await(task.register) shouldBe true
      verify(
        1,
        postRequestedFor(urlMatching("/registration"))
          .withHeader("content-type", equalTo("application/json"))
          .withRequestBody(equalTo(regPayloadStringFor("mobile-paye", "https://mobile-paye.protected.mdtp")))
      )
    }

    "handle errors" in {
      registrationWillFail()
      await(task.register) shouldBe false
    }
  }
}
