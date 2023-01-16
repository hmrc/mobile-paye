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

import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mobilepaye.repository.admin.AdminRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration, SECONDS => Seconds}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureFlagService @Inject()(
  adminRepository: AdminRepository,
  cache: AsyncCacheApi
)(
  implicit ec: ExecutionContext
) {
  val cacheValidFor: FiniteDuration =
    Duration(2, Seconds)

  private val allFeatureFlagsCacheKey = "*$*$allFeatureFlags*$*$"

  def getAll: Future[List[FeatureFlag]] =
    cache
      .getOrElseUpdate(allFeatureFlagsCacheKey, cacheValidFor) {
        adminRepository
          .getFeatureFlags
          .map {
            flagsFromMongo =>
              FeatureFlagName
                .flags
                .foldLeft(flagsFromMongo) {
                  (featureFlags, missingFlag) =>
                    if (featureFlags.map(_.name).contains(missingFlag))
                      featureFlags
                    else
                      FeatureFlag(name = missingFlag, isEnabled = false) :: featureFlags
                }.reverse
          }
      }

  def setAll(flags: Map[FeatureFlagName, Boolean]): Future[Unit] =
    Future
      .sequence(
        flags.keys.map(flag => cache.remove(flag.toString))
      ) flatMap {
        _ =>
          cache.remove(allFeatureFlagsCacheKey)
          adminRepository.setFeatureFlags(flags)
      } map {
        _ =>
          //blocking thread to allow other containers to update their cache
          Thread.sleep(5000)
          ()
      }

  def set(flagName: FeatureFlagName, enabled: Boolean): Future[Boolean] =
    for {
      _ <- cache.remove(flagName.toString)
      _ <- cache.remove(allFeatureFlagsCacheKey)
      result <- adminRepository.setFeatureFlag(flagName, enabled)
      //blocking thread to allow other containers to update their cache
      _ <- Future.successful(Thread.sleep(5000))
    } yield result

  def get(flagName: FeatureFlagName): Future[FeatureFlag] =
    cache
      .getOrElseUpdate(flagName.toString, cacheValidFor) {
        adminRepository
          .getFeatureFlag(flagName)
          .map(_.getOrElse(FeatureFlag(name = flagName, isEnabled = false)))
      }

}
