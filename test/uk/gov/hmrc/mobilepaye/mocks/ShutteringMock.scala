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

package uk.gov.hmrc.mobilepaye.mocks

import org.scalamock.handlers.CallHandler
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.connectors.ShutteringConnector
import uk.gov.hmrc.mobilepaye.domain.Shuttering
import uk.gov.hmrc.mobilepaye.domain.types.JourneyId
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

trait ShutteringMock extends BaseSpec {

  def mockShutteringResponse(
    response: Shuttering
  )(implicit shutteringConnector: ShutteringConnector): CallHandler[Future[Shuttering]] =
    (shutteringConnector
      .getShutteringStatus(_: JourneyId, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future successful response)

}
