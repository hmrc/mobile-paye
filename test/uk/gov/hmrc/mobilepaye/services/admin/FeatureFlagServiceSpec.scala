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

package uk.gov.hmrc.mobilepaye.services.admin

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import uk.gov.hmrc.mobilepaye.domain.admin.OnlinePaymentIntegration
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mobilepaye.repository.admin.AdminRepository
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.Future
import scala.reflect.ClassTag

class FeatureFlagServiceSpec
  extends BaseSpec
    with ScalaFutures
    with BeforeAndAfterEach {

  implicit private val arbitraryFeatureFlagName: Arbitrary[FeatureFlagName] =
    Arbitrary {
      Gen.oneOf(FeatureFlagName.flags)
    }

  def mock[T](implicit ev: ClassTag[T]): T =
    Mockito.mock(ev.runtimeClass.asInstanceOf[Class[T]])

  val adminRepository: AdminRepository = mock[AdminRepository]
  val service = new FeatureFlagService(adminRepository, mockCacheApi)

  override def beforeEach(): Unit = {
    reset(adminRepository)
  }

  "When set works in the repo returns true" in {
    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(List.empty))
    when(adminRepository.setFeatureFlags(any())).thenReturn(Future.successful(()))

    val flagName = arbitrary[FeatureFlagName].sample.getOrElse(OnlinePaymentIntegration)

    val result = service.set(flagName, enabled = true).futureValue

    result mustBe true

    val captor = ArgumentCaptor.forClass(classOf[Map[FeatureFlagName, Boolean]])

    verify(adminRepository, times(1)).setFeatureFlags(captor.capture())

    captor.getValue must contain(FeatureFlag(OnlinePaymentIntegration, isEnabled = true))
  }

  "When set fails in the repo returns false" in {
    val flagName = arbitrary[FeatureFlagName].sample.getOrElse(OnlinePaymentIntegration)

    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(List.empty))
    when(adminRepository.setFeatureFlags(any())).thenReturn(Future.successful(()))

    service.set(flagName, enabled = true).futureValue mustBe false
  }

  "When getAll is called returns all of the flags from the repo" in {
    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(List.empty))

    service.getAll.futureValue mustBe List(FeatureFlag(OnlinePaymentIntegration, isEnabled = false))
  }
}
