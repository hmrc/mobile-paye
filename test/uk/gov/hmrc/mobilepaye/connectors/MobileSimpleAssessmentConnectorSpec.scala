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


import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, HttpReads, NotFoundException}
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, FeatureFlagName, OnlinePaymentIntegration}
import uk.gov.hmrc.mobilepaye.services.admin.FeatureFlagService
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.{ExecutionContext, Future}

class MobileSimpleAssessmentConnectorSpec extends BaseSpec with LogCapturing {
  val mockCoreGet:            CoreGet            = mock[CoreGet]
  val serviceUrl:             String             = "mobile-simple-assessment"
  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  val connector: MobileSimpleAssessmentConnector =
    MobileSimpleAssessmentConnector(mockCoreGet, serviceUrl, mockFeatureFlagService)

  def mockMobileSimpleAssessmentGet[T](f: Future[T]) =
    (mockCoreGet
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[T],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(s"$serviceUrl/liabilities?journeyId=$journeyId", *, *, *, *, *)
      .returning(f)

  def mockFeatureFlagGet =
    (mockFeatureFlagService
      .get(_: FeatureFlagName))
      .expects(OnlinePaymentIntegration)
      .returning(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))

  "getSimpleAssessmentLiabilities" should {

    "not fail or log for NotFoundResponse" in {
      mockFeatureFlagGet
      mockMobileSimpleAssessmentGet(Future.failed(new NotFoundException("")))

      withCaptureOfLoggingFrom(connector.logger) { events =>
        val result = await(connector.getSimpleAssessmentLiabilities(journeyId))
        events.size shouldBe 0
        result      shouldBe None
      }
    }

    "not fail for some Throwable, but log details" in {
      mockFeatureFlagGet
      mockMobileSimpleAssessmentGet(Future.failed(new InternalError("Internal Error")))
      withCaptureOfLoggingFrom(connector.logger) { events =>
        val result = await(connector.getSimpleAssessmentLiabilities(journeyId))
        events.size            shouldBe 1
        events.head.getMessage shouldBe "Call to mobile-simple-assessment failed. Reason: java.lang.InternalError: Internal Error Boxed Exception"
        result                 shouldBe None
      }
    }
  }
}
