/*
 * Copyright 2025 HM Revenue & Customs
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

import uk.gov.hmrc.http.*
import uk.gov.hmrc.mobilepaye.domain.admin.OnlinePaymentIntegration
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.TaxYearReconciliation
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class TaxCalcConnectorSpec extends BaseSpec {
  override val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  override val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val serviceUrl: String = "https://tax-calc-url"

  val connector: TaxCalcConnector = new TaxCalcConnector(mockHttpClient, serviceUrl, mockFeatureFlagService)

  def mockTaxCalcGet[T](f: Future[T]) = {

    (mockHttpClient
      .get(_: URL)(_: HeaderCarrier))
      .expects(url"$serviceUrl/taxcalc/${nino.value}/reconciliations", *)
      .returning(mockRequestBuilder)

    (mockRequestBuilder
      .execute[T](using _: HttpReads[T], _: ExecutionContext))
      .expects(*, *)
      .returns(f)
  }

  "getTaxReconciliations" should {
    "not fail for some Throwable" in {

      (mockFeatureFlagService
        .get(_: FeatureFlagName))
        .expects(OnlinePaymentIntegration)
        .returning(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
      mockTaxCalcGet(Future.failed(new Throwable("")))

      val result: Option[List[TaxYearReconciliation]] = await(connector.getTaxReconciliations(nino))
      result shouldBe None
    }
  }
}
