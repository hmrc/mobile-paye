import play.api.libs.ws.WSRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseISpec

class LiveMobilePayeControllerISpec extends BaseISpec {

  "GET /:nino/paye-data" should {
    def request(nino: Nino): WSRequest = wsUrl(s"/${nino.value}/paye-data").withHeaders(acceptJsonHeader)

    "return OK and 'Hello world'" in {
      val response = await(request(nino).get())
      response.status shouldBe 200
      response.body should include("Hello world")
    }
  }

}
