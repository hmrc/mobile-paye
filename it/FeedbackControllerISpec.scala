
import play.api.test.Injecting
import utils.BaseISpec
import stubs.AuthStub._
import stubs.FeedbackStub.postFeedbackStub
import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig

class FeedbackControllerISpec extends BaseISpec with Injecting {

  val appConfig: MobilePayeConfig = MobilePayeConfig(app.configuration)
  lazy val url = "/feedback"

  "POST /feedback" should {

    "return 204 No Content" in {
      grantAccessNoNino()
      postFeedbackStub()
      val response = await(postRequestWithAuthHeaders(url, feedbackJson))

      response.status shouldBe 204

    }
  }

}
