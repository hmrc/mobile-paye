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

import akka.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.mobilepaye.domain.admin.FeatureFlag.{Disabled, Enabled}
import uk.gov.hmrc.mobilepaye.domain.admin.FeatureFlagName.OnlinePaymentIntegration
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mobilepaye.repository.admin.AdminRepository
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.Future
import scala.concurrent.duration.Duration
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

  // A cache that doesn't cache
  class FakeCache extends AsyncCacheApi {
    override def set(key: String, value: Any, expiration: Duration): Future[Done] = ???

    override def remove(key: String): Future[Done] = ???

    override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A])(implicit evidence$1: ClassTag[A]): Future[A] = orElse

    override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Future[Option[T]] = ???

    override def removeAll(): Future[Done] = ???
  }

  val adminRepository: AdminRepository = mock[AdminRepository]
  val service = new FeatureFlagService(adminRepository, new FakeCache())

  override def beforeEach(): Unit = {
    reset(adminRepository)
  }

  "When set works in the repo returns true" in {
    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(Some(Seq.empty)))
    when(adminRepository.setFeatureFlags(any())).thenReturn(Future.successful(true))

    val flagName = arbitrary[FeatureFlagName].sample.getOrElse(FeatureFlagName.OnlinePaymentIntegration)

    val result = service.set(flagName, enabled = true).futureValue

    result mustBe true

    val captor = ArgumentCaptor.forClass(classOf[Seq[FeatureFlag]])

    verify(adminRepository, times(1)).setFeatureFlags(captor.capture())

    captor.getValue must contain(Enabled(flagName))
  }

  "When set fails in the repo returns false" in {
    val flagName = arbitrary[FeatureFlagName].sample.getOrElse(FeatureFlagName.OnlinePaymentIntegration)

    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(Some(Seq.empty)))
    when(adminRepository.setFeatureFlags(any())).thenReturn(Future.successful(false))

    service.set(flagName, enabled = true).futureValue mustBe false
  }

  "When getAll is called returns all of the flags from the repo" in {
    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(Some(Seq.empty)))

    service.getAll.futureValue mustBe Seq(Enabled(OnlinePaymentIntegration))
  }

  "When a flag doesn't exist in the repo the default is returned" in {
    when(adminRepository.getFeatureFlags).thenReturn(Future.successful(Some(Seq.empty)))

    service.get(OnlinePaymentIntegration).futureValue mustBe Enabled(OnlinePaymentIntegration)
  }

  "When a flag exists in the repo that overrides the default" in {
    when(adminRepository.getFeatureFlags)
      .thenReturn(Future.successful(Some(Seq(Disabled(OnlinePaymentIntegration)))))

    service.get(OnlinePaymentIntegration).futureValue mustBe Disabled(OnlinePaymentIntegration)
  }
}
