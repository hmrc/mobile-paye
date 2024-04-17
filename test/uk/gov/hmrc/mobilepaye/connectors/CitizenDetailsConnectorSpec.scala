/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.http.{CoreGet, ForbiddenException, HeaderCarrier, HttpReads, InternalServerException, TooManyRequestException, UnauthorizedException}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import scala.concurrent.{ExecutionContext, Future}

class CitizenDetailsConnectorSpec extends BaseSpec {

  val mockCoreGet: CoreGet                 = mock[CoreGet]
  val serviceUrl:  String                  = "tst-url"
  val connector:   CitizenDetailsConnector = new CitizenDetailsConnector(serviceUrl, mockCoreGet)

  def mockCiDGet[T](f: Future[T]) =
    (mockCoreGet
      .GET(_: String, _: Seq[(String, String)], _: Seq[(String, String)])(_: HttpReads[T],
                                                                          _: HeaderCarrier,
                                                                          _: ExecutionContext))
      .expects(s"$serviceUrl/citizen-details/$nino/designatory-details/basic", *, *, *, *, *)
      .returning(f)

  "throw UnauthorisedException for valid nino but unauthorized user" in {
    mockCiDGet(Future.failed(new UnauthorizedException("Unauthorized")))

    intercept[UnauthorizedException] {
      await(connector.getPerson(nino))
    }
  }

  "throw ForbiddenException for valid nino for authorised user but for a different nino" in {
    mockCiDGet(Future.failed(new ForbiddenException("Forbidden")))

    intercept[ForbiddenException] {
      await(connector.getPerson(nino))
    }
  }

  "throw InternalServerException for valid nino for authorised user when receiving a 500 response from tai" in {
    mockCiDGet(Future.failed(new InternalServerException("Internal Server Error")))

    intercept[InternalServerException] {
      await(connector.getPerson(nino))
    }
  }

  "throw TooManyRequests Exception for valid nino for authorised user when receiving a 429 response from tai" in {
    mockCiDGet(Future.failed(new TooManyRequestException("TOO_MANY_REQUESTS")))

    intercept[TooManyRequestException] {
      await(connector.getPerson(nino))
    }
  }

}
