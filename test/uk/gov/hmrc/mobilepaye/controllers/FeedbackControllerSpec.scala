/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mobilepaye.controllers

import org.scalamock.handlers._
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.ConfidenceLevel.L50
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilepaye.domain.Feedback
import uk.gov.hmrc.mobilepaye.mocks.AuthorisationNoNinoMock
import uk.gov.hmrc.mobilepaye.services.MobilePayeService
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import eu.timepit.refined.auto._
import uk.gov.hmrc.mobilepaye.controllers.BadRequestException

import scala.concurrent.Future

class FeedbackControllerSpec extends BaseSpec with AuthorisationNoNinoMock{

  private val fakeRequest = FakeRequest("POST", "/", Headers((CONTENT_TYPE, JSON)), feedbackJson).withHeaders(acceptHeader)
  val confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200
  val mockMobilePayeService: MobilePayeService = mock[MobilePayeService]
  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]


  def controller =
    new FeedbackController(
      mockAuthConnector,
      mockMobilePayeService,
      stubControllerComponents(),
      confLevel = 200
    )

  def mockPostMobilePayeFeedback(f: Future[HttpResponse]): CallHandler2[Feedback, HeaderCarrier, Future[HttpResponse]] =
    (mockMobilePayeService
      .postFeedback(_: Feedback)(_: HeaderCarrier))
      .expects(*, *)
      .returning(f)

  "postFeedback" should {

    "return 204 No Content when a valid feedback model is provided" in {
      mockPostMobilePayeFeedback(Future.successful(HttpResponse.apply(204,"")))
      mockAuthorisationNoNinoGrantAccess(confidenceLevel)

      val result = controller.postFeedback("9bcb9c5a-0cfd-49e3-a935-58a28c386a42")(fakeRequest)

      status(result) shouldBe 204
    }

    "return 500 when the feedback service throws an InternalServerException" in {
      mockAuthorisationNoNinoGrantAccess(confidenceLevel)
      mockPostMobilePayeFeedback(Future.failed(new InternalServerException("Internal Server Error")))

      val result = controller.postFeedback("9bcb9c5a-0cfd-49e3-a935-58a28c386a42")(fakeRequest)

      status(result) shouldBe 500
    }

    "return 401 for valid feedback model but low CL" in {
      mockAuthorisationNoNinoGrantAccess(L50)

      val result = controller.postFeedback("9bcb9c5a-0cfd-49e3-a935-58a28c386a42")(fakeRequest)

      status(result) shouldBe 401
    }

    "return 400 when handling BadRequestException" in {
      mockAuthorisationNoNinoGrantAccess(confidenceLevel)
      mockPostMobilePayeFeedback(Future.failed(new BadRequestException("Bad Request")))

      val result = controller.postFeedback("9bcb9c5a-0cfd-49e3-a935-58a28c386a42")(fakeRequest)

      status(result) shouldBe 400
    }

    "return 401 when handling 401 Upstream4xxResponse" in {
      mockAuthorisationNoNinoGrantAccess(confidenceLevel)
      mockPostMobilePayeFeedback(Future.failed(UpstreamErrorResponse.apply("Upstream Exception",401)))

      val result = controller.postFeedback("9bcb9c5a-0cfd-49e3-a935-58a28c386a42")(fakeRequest)

      status(result) shouldBe 401
    }

    "return 401 when handling AuthorisedException" in {
      mockAuthorisationNoNinoGrantAccess(confidenceLevel)
      mockPostMobilePayeFeedback(Future.failed(new MissingBearerToken))

      val result = controller.postFeedback("9bcb9c5a-0cfd-49e3-a935-58a28c386a42")(fakeRequest)

      status(result) shouldBe 401
    }

  }
}
