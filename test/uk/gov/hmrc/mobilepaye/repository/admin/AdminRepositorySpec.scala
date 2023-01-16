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

package uk.gov.hmrc.mobilepaye.repository.admin

import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, FeatureFlagName, OnlinePaymentIntegration}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.Future

class AdminRepositorySpec
  extends BaseSpec
    with DefaultPlayMongoRepositorySupport[FeatureFlag] {

  override lazy val repository: AdminRepository =
    new AdminRepository(mongoComponent)

  def insertRecord(
    flag: FeatureFlagName = OnlinePaymentIntegration,
    enabled: Boolean = true
  ): Future[Boolean] =
    insert(
      FeatureFlag(flag, enabled, flag.description)
    ).map(_.wasAcknowledged())

  "getFlag" should {
    "return None if there is no record" in {
      val result = repository.getFeatureFlag(OnlinePaymentIntegration).futureValue

      result shouldBe None
    }
  }

  "setFeatureFlag and getFeatureFlag" should {
    "insert and read a record in mongo" in {
      val result = (for {
        _      <- insertRecord()
        result <- repository.getFeatureFlag(name = OnlinePaymentIntegration)
      } yield result).futureValue

      result shouldBe Some(
        FeatureFlag(
          name        = OnlinePaymentIntegration,
          isEnabled   = true,
          description = OnlinePaymentIntegration.description
        )
      )
    }
  }

  "setFeatureFlag" should {
    "replace a record not create a new one" in {
      val result = (for {
        _      <- repository.setFeatureFlag(OnlinePaymentIntegration, enabled = true)
        _      <- repository.setFeatureFlag(OnlinePaymentIntegration, enabled = false)
        result <- find(Filters.equal("name", OnlinePaymentIntegration.toString))
      } yield result).futureValue

      result.length shouldBe 1
      result.head.isEnabled shouldBe false
    }
  }

  "setFeatureFlags" should {
    "not create duplicates" in {
      val result = (for {
        _      <- repository.setFeatureFlags(Map(OnlinePaymentIntegration -> true, OnlinePaymentIntegration -> false))
        result <- find(Filters.equal("name", OnlinePaymentIntegration.toString))
      } yield result).futureValue

      result.length shouldBe 1
      result.head.isEnabled shouldBe false
    }
  }

  "getAllFeatureFlags" should {
    "get a list of all the feature toggles" in {
      val allFlags: Seq[FeatureFlag] = (for {
        _      <- insertRecord()
        result <- repository.getFeatureFlags
      } yield result).futureValue

      allFlags shouldBe List(
        FeatureFlag(
          name        = OnlinePaymentIntegration,
          isEnabled   = true,
          description = OnlinePaymentIntegration.description
        )
      )
    }
  }

  "Collection" should {
    "not allow duplicates" in {
      val result = intercept[MongoWriteException] {
        await(for {
          _ <- insertRecord()
          _ <- insertRecord(enabled = false)
        } yield true)
      }

      result.getCode shouldBe 11000
      result.getError.getMessage shouldBe
        s"""E11000 duplicate key error collection: test-AdminRepositorySpec.admin-feature-flags index: name dup key: { name: "$OnlinePaymentIntegration" }"""
    }
  }
}
