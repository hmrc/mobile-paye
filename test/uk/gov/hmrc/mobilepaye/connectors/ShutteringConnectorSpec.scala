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
import uk.gov.hmrc.mobilepaye.domain.Shuttering
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import eu.timepit.refined.auto.*
import org.mockito.ArgumentMatchers.{any, anyBoolean}
import org.mockito.Mockito
import org.mockito.Mockito.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.mobilepaye.domain.types.JourneyId

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ShutteringConnectorSpec extends BaseSpec {

  override val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  override val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val serviceUrl: String = "https://mobile-simple-assessment"
  val connector: ShutteringConnector = new ShutteringConnector(mockHttpClient, serviceUrl)

  val jid: JourneyId = JourneyId.from("27085215-69a4-4027-8f72-b04b10ec16b0").toOption.get

  def mockShutteringGet[T](f: Future[T]) = {
    (mockHttpClient
      .get(_: URL)(_: HeaderCarrier))
      .expects(
        url"$serviceUrl/mobile-shuttering/service/mobile-paye/shuttered-status?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0",
        *
      )
      .returning(mockRequestBuilder)
    (mockRequestBuilder
      .execute[T](using _: HttpReads[T], _: ExecutionContext))
      .expects(*, *)
      .returns(f)
  }

  "getTaxReconciliations" should {
    "Assume unshuttered for InternalServerException response" in {
      mockShutteringGet(Future.successful(new InternalServerException("")))
      connector.getShutteringStatus(jid) onComplete {
        case Success(value) => value shouldBe Shuttering.shutteringDisabled
        case Failure(_)     =>
      }

    }

    "Assume unshuttered for BadGatewayException response" in {
      mockShutteringGet(Future.successful(new BadGatewayException("")))

      connector.getShutteringStatus(jid) onComplete {
        case Success(value) => value shouldBe Shuttering.shutteringDisabled
        case Failure(_)     =>
      }

    }
  }
}
