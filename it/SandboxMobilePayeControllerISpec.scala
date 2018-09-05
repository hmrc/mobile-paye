import play.api.libs.ws.WSRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseISpec

class SandboxMobilePayeControllerISpec extends BaseISpec {

  private val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"

  "GET /sandbox/:nino/paye-data" should {
    def request(nino: Nino): WSRequest = wsUrl(s"/${nino.value}/paye-data").withHeaders(acceptJsonHeader)

    "return OK and 'Hello world sandbox'" in {
      val response = await(request(nino).withHeaders(mobileHeader).get())
      response.status shouldBe 200
      response.body should include("Hello world sandbox")
    }
  }

}
