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

package uk.gov.hmrc.mobilepaye.utils

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{ActorMaterializer, Materializer}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.{Configuration, Environment}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.MobilePayeTestData
import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig
import uk.gov.hmrc.mobilepaye.domain.types.JourneyId
import uk.gov.hmrc.mobilepaye.mocks.{AuditMock, AuthorisationMock, ShutteringMock}
import eu.timepit.refined.auto.*
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Args, Outcome, Status}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mobilepaye.services.admin.FeatureFlagService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

trait BaseSpec extends AnyWordSpecLike with Matchers with FutureAwaits with DefaultAwaitTimeout with MobilePayeTestData with MockFactory {
  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit lazy val system: ActorSystem = ActorSystem()
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  private lazy val env = Environment.simple()
  private lazy val configuration = Configuration.load(env)
  implicit lazy val appConfig: MobilePayeConfig = new MobilePayeConfig(configuration)
  val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]
  implicit val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"
  val journeyId: JourneyId = JourneyId.from("27085215-69a4-4027-8f72-b04b10ec16b0").toOption.get

}

trait PlayMongoRepositorySupport[A] extends DefaultPlayMongoRepositorySupport[A] {
  override def withFixture(test: NoArgTest): Outcome = super.withFixture(test)
  override protected def checkTtlIndex: Boolean = false

}
