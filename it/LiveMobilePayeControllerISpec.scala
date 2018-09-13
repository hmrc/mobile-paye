import play.api.libs.ws.WSRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseISpec

class LiveMobilePayeControllerISpec extends BaseISpec {

  "GET /summary" should {
    val request: WSRequest = wsUrl("/summary").withHeaders(acceptJsonHeader)

    "return OK and 'Hello world'" in {
      val response = await(request.get())
      response.status shouldBe 200
      response.body should include("Hello world")
    }
  }
}