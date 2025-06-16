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

import org.mockito.Mockito.*
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, HttpReads, InternalServerException, StringContextOps, TooManyRequestException, UnauthorizedException}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CitizenDetailsConnectorSpec extends BaseSpec {

  val serviceUrl: String = "https://www.tst-url.com"
  val connector: CitizenDetailsConnector = new CitizenDetailsConnector(serviceUrl, mockHttpClient)

  def mockCiDGet[T](f: Future[T]) = {
    (mockHttpClient
      .get(_: URL)(_: HeaderCarrier))
      .expects(url"$serviceUrl/citizen-details/$nino/designatory-details/basic", *)
      .returning(mockRequestBuilder)

    (mockRequestBuilder
      .execute[T](using _: HttpReads[T], _: ExecutionContext))
      .expects(*, *)
      .returns(f)
  }

  "throw UnauthorisedException for valid nino but unauthorized user" in {
    mockCiDGet(Future.failed(new UnauthorizedException("Unauthorized")))

    connector.getPerson(nino) onComplete {
      case Success(_) => fail()
      case Failure(_) =>
    }
  }

  "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
    mockCiDGet(Future.failed(new ForbiddenException("Forbidden")))

    connector.getPerson(nino) onComplete {
      case Success(_) => fail()
      case Failure(_) =>
    }

  }

  "throw InternalServerException for valid nino for authorised user when receiving a 500 response from tai" in {
    mockCiDGet(Future.failed(new InternalServerException("Internal Server Error")))

    connector.getPerson(nino) onComplete {
      case Success(_) => fail()
      case Failure(_) =>
    }

  }

  "throw TooManyRequests Exception for valid nino for authorised user when receiving a 429 response from tai" in {
    mockCiDGet(Future.failed(new TooManyRequestException("TOO_MANY_REQUESTS")))

    connector.getPerson(nino) onComplete {
      case Success(_) => fail()
      case Failure(_) =>
    }

  }

}
