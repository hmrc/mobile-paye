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

package uk.gov.hmrc.mobilepaye.connectors

import play.api.libs.json.Writes
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import play.api.http.Status

import scala.concurrent.{ExecutionContext, Future}

class FeedbackConnectorSpec extends BaseSpec{

  val mockHttpClient: HttpClient = mock[HttpClient]

  val serviceUrl = "mobile-feedback-url"

  val connector = new FeedbackConnector(mockHttpClient, serviceUrl)

  def performSuccessfulPOST[I, O](response: Future[O])(implicit http: HttpClient): Unit =
    (
      http
        .POST[I, O](_: String, _: I, _: Seq[(String, String)])(
          _: Writes[I],
          _: HttpReads[O],
          _: HeaderCarrier,
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *, *, *, *)
      .returns(response)

  def performUnsuccessfulPOST[I, O](response: Exception)(implicit http: HttpClient): Unit =
    (
      http
        .POST[I, O](_: String, _: I, _: Seq[(String, String)])(
          _: Writes[I],
          _: HttpReads[O],
          _: HeaderCarrier,
          _: ExecutionContext
        )
      )
      .expects(*, *, *, *, *, *, *)
      .returns(Future failed response)

  "Calling .postFeedback" when {

    "postFeeback when a valid feedback model is provided" should {

      "return a 204 No Content" in {

        performSuccessfulPOST(Future successful HttpResponse.apply(204,""))(mockHttpClient)
        val result = await(connector.postFeedback(feedbackModel))
        result.status shouldBe Status.NO_CONTENT

      }

      "throw UnauthorisedException for valid feedback model but unauthorized user" in {
        performUnsuccessfulPOST(new UnauthorizedException("unauthorized"))(mockHttpClient)

        intercept[UnauthorizedException] {
          await(connector.postFeedback(feedbackModel))
        }
      }

      "throw ForbiddenException for valid feedback model but different nino" in {
        performUnsuccessfulPOST(new ForbiddenException("forbidden"))(mockHttpClient)

        intercept[ForbiddenException] {
          await(connector.postFeedback(feedbackModel))
        }
      }

      "throw InternalServerException for valid nino for authorised user when receiving a 500 response from tai" in {
        performUnsuccessfulPOST(new InternalServerException("Internal Server Error"))(mockHttpClient)

        intercept[InternalServerException] {
          await(connector.postFeedback(feedbackModel))
        }
      }

    }

  }


}
