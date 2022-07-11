/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.controllers.admin

import akka.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.cache.AsyncCacheApi
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsBoolean
import play.api.mvc.AnyContentAsEmpty
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.mobilepaye.domain.admin.FeatureFlagName.OnlinePaymentIntegration
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mobilepaye.repository.admin.AdminRepository
import uk.gov.hmrc.mobilepaye.services.admin.FeatureFlagService
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class FeatureFlagControllerSpec
  extends BaseSpec
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach {

  def fakeCSRFRequest(method: String, uri: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, uri).withCSRFToken.asInstanceOf[FakeRequest[AnyContentAsEmpty.type]]

  // A cache that doesn't cache
  val mockCacheApi: AsyncCacheApi = new AsyncCacheApi {
    override def set(key: String, value: Any, expiration: Duration): Future[Done] = ???

    override def remove(key: String): Future[Done] = ???

    override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A])(implicit evidence$1: ClassTag[A]): Future[A] = orElse

    override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Future[Option[T]] = ???

    override def removeAll(): Future[Done] = ???
  }

  def mock[T](implicit ev: ClassTag[T]): T =
    Mockito.mock(ev.runtimeClass.asInstanceOf[Class[T]])

  val mockAdminRepository: AdminRepository = mock[AdminRepository]

  val mockFeatureFlagService: FeatureFlagService = mock[FeatureFlagService]

  val flags: Seq[FeatureFlag] =
    Seq(FeatureFlag(OnlinePaymentIntegration, enabled = true))

  def application: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AsyncCacheApi].toInstance(mockCacheApi),
        bind[AdminRepository].to(mockAdminRepository),
        bind[FeatureFlagService].to(mockFeatureFlagService)
      )

  override def beforeEach(): Unit = {
    reset(
      mockAdminRepository,
      mockFeatureFlagService
    )
  }

  "Feature Flag Controller" should {

    "return OK with the service configuration for a GET request" in {
      when(mockFeatureFlagService.getAll).thenReturn(Future.successful(flags))

      val app = application.build()

      running(app) {
        val request = fakeCSRFRequest(GET, routes.FeatureFlagController.get.url)

        val result = route(app, request).head

        status(result) shouldBe OK
      }
    }

    "return NoContent for a successful PUT request" in {
      when(mockFeatureFlagService.set(any(), any())).thenReturn(Future.successful(true))

      val app = application.build()

      running(app) {
        val request = fakeCSRFRequest(
          PUT,
          routes.FeatureFlagController.put(FeatureFlagName.OnlinePaymentIntegration).url
        ).withBody(JsBoolean(true))

        val result = route(app, request).head

        status(result) shouldBe NO_CONTENT
      }
    }

    "return BadRequest for a PUT request with an invalid payload" in {
      when(mockFeatureFlagService.set(any(), any())).thenReturn(Future.successful(false))

      val app = application.build()

      running(app) {
        val request = fakeCSRFRequest(
          PUT,
          routes.FeatureFlagController.put(FeatureFlagName.OnlinePaymentIntegration).url
        ).withBody("This is not a valid request body")

        val result = route(app, request).head

        status(result) shouldBe BAD_REQUEST
      }
    }

  }

}
