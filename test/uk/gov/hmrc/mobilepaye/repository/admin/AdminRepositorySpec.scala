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

package uk.gov.hmrc.mobilepaye.repository.admin

import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, OnlinePaymentIntegration}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.mobilepaye.utils.admin.AdminRepositoryUtils
import uk.gov.hmrc.mongo.cache.SessionCacheRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class AdminRepositorySpec
  extends BaseSpec
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[FeatureFlag] {

  override protected lazy val optSchema: Option[BsonDocument] =
    Some(BsonDocument(
      """{
        "bsonType": "object",
        "required": [
          "_id",
          "name",
          "isEnabled"
        ],
        "properties": {
          "_id": {
            "bsonType": "objectId"
          },
          "name": {
            "bsonType": "string"
          },
          "isEnabled": {
            "bsonType": "bool"
          },
          "description": {
            "bsonType": "string"
          }
        }
      }"""
    ))

  def application: Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[AsyncCacheApi].toInstance(mockCacheApi)
      )
      .configure(Map("mongodb.uri" -> mongoUri))
      .build()

  lazy val repository: AdminRepository =
    application.injector.instanceOf[AdminRepository]

  "getFlag" should {
    "return None if there is no record" in {
      val result = repository.getFeatureFlag(OnlinePaymentIntegration).futureValue

      result mustBe None
    }
  }

  "setFeatureFlag and getFeatureFlag" should {
    "insert and read a record in mongo" in {
      val result = (for {
        _      <- repository.setFeatureFlag(name = OnlinePaymentIntegration, enabled = true)
        result <- repository.getFeatureFlag(name = OnlinePaymentIntegration)
      } yield result).futureValue

      result mustBe Some(
        FeatureFlag(
          name        = OnlinePaymentIntegration,
          isEnabled   = true,
          description = OnlinePaymentIntegration.description
        )
      )
    }
  }

  "getAllFeatureFlags" should {
    "get a list of all the feature toggles" in {
      val allFlags: Seq[FeatureFlag] = (for {
        _      <- repository.setFeatureFlag(name = OnlinePaymentIntegration, enabled = true)
        result <- repository.getFeatureFlags
      } yield result).futureValue

      allFlags mustBe List(
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
      lazy val adminRepositoryUtils = app.injector.instanceOf[AdminRepositoryUtils]

      val result = intercept[MongoWriteException] {
        await(for {
          _ <- adminRepositoryUtils.insertFeatureFlag(OnlinePaymentIntegration, enabled = true)
          _ <- adminRepositoryUtils.insertFeatureFlag(OnlinePaymentIntegration, enabled = false)
        } yield true)
      }

      result.getCode mustBe 11000
      result.getError.getMessage mustBe
        s"""E11000 duplicate key error collection: mobile-paye.admin-feature-flags index: name dup key: { name: "$OnlinePaymentIntegration" }"""
    }
  }
}
