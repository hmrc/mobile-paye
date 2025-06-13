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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

trait AuthorisationNoNinoMock extends BaseSpec {

  type GrantAccessNoNino = ConfidenceLevel

  def mockAuthorisationNoNinoGrantAccess(
                                          response: GrantAccessNoNino
                                        )(implicit authConnector: AuthConnector
                                        ): CallHandler[Future[GrantAccessNoNino]] =
    (authConnector
      .authorise(_: Predicate, _: Retrieval[GrantAccessNoNino])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future successful response)

//  def mockAuthorisationNoNinoGrantAccess(
//    response: GrantAccessNoNino
//  )(implicit authConnector: AuthConnector) =
//    when(
//      authConnector
//        .authorise[GrantAccessNoNino](any(), any())(any(), any())
//    )
//      .thenReturn(Future successful response)
}
