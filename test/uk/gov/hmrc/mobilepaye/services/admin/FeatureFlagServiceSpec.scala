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

package uk.gov.hmrc.mobilepaye.services.admin

import org.mockito.ArgumentMatchers.{any, anyBoolean}
import org.mockito.Mockito
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.*
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, FeatureFlagName, OnlinePaymentIntegration}
import uk.gov.hmrc.mobilepaye.repository.admin.AdminRepository
import uk.gov.hmrc.mobilepaye.utils.{BaseSpec, MockAsyncCacheApi}

import scala.concurrent.Future
import scala.reflect.ClassTag

class FeatureFlagServiceSpec extends BaseSpec with BeforeAndAfterEach {

  def mock[T](implicit ev: ClassTag[T]): T =
    Mockito.mock(ev.runtimeClass.asInstanceOf[Class[T]])
  val mockCacheApi: MockAsyncCacheApi =
    new MockAsyncCacheApi()
  lazy val mockAdminRepository: AdminRepository =
    mock[AdminRepository]
  val service: FeatureFlagService =
    new FeatureFlagService(mockAdminRepository, mockCacheApi)
  def flag(isEnabled: Boolean = true): FeatureFlag =
    FeatureFlag(OnlinePaymentIntegration, isEnabled)

  override def beforeEach(): Unit = {
    reset(mockAdminRepository)
    when(mockAdminRepository.getFeatureFlags).thenReturn(Future.successful(List.empty))
  }

  "When set works in the repo returns true" in {
    when(mockAdminRepository.setFeatureFlag(any(), any())).thenReturn(Future.successful(true))

    await(service.set(OnlinePaymentIntegration, enabled = true)) mustBe true

    verify(mockAdminRepository, times(1)).setFeatureFlag(any[FeatureFlagName](), anyBoolean())
  }

  "When set fails in the repo returns false" in {
    when(mockAdminRepository.setFeatureFlag(any(), any())).thenReturn(Future.successful(false))

    await(service.set(OnlinePaymentIntegration, enabled = true)) mustBe false
  }

  "When get works return feature the requested flag" in {
    when(mockAdminRepository.getFeatureFlag(any())).thenReturn(Future.successful(Some(flag())))

    await(service.get(OnlinePaymentIntegration)) mustBe flag()
  }

  "When get fails return feature the requested flag with value false" in {
    when(mockAdminRepository.getFeatureFlag(any())).thenReturn(Future.successful(None))

    await(service.get(OnlinePaymentIntegration)) mustBe flag(false)
  }

  "When getAll is called returns all of the flags from the repo" in {
    when(mockAdminRepository.getFeatureFlags).thenReturn(Future.successful(List(flag())))

    await(service.getAll) mustBe List(FeatureFlag(OnlinePaymentIntegration, isEnabled = true))
  }

  "When getAll is called and repo is empty returns all of the flags as false" in {
    await(service.getAll) mustBe List(FeatureFlag(OnlinePaymentIntegration, isEnabled = false))
  }

  "When setAll is called return successful Unit" in {
    when(mockAdminRepository.setFeatureFlags(any())).thenReturn(Future.successful(()))

    await(service.setAll(Map(flag().name -> flag().isEnabled))) mustBe ((): Unit)
  }
}
