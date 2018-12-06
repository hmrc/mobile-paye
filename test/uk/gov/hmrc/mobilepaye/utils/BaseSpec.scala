/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.utils

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.MobilePayeTestData
import uk.gov.hmrc.mobilepaye.mocks.AuthorisationMock
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext


trait BaseSpec extends UnitSpec with WithFakeApplication with MobilePayeTestData with AuthorisationMock {
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val system = ActorSystem()
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

}