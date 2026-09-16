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

package uk.gov.hmrc.mobilepaye.repository

import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig
import uk.gov.hmrc.mobilepaye.domain.{P800Cache, P800CacheHashNino}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.serviceResponse.Response

import scala.concurrent.ExecutionContext

class P800CacheMongoSpec extends AnyWordSpec with Matchers with ScalaFutures with DefaultPlayMongoRepositorySupport[P800CacheHashNino] {
  val config: Configuration = Configuration(
    ConfigFactory.parseString(s"""
                                 |microservice.services.internal-auth.resource-type=localhostHip
                                 |mongodb.ttlSecond=86400
                                 |mongodb.hashKey=c29tZS1sb25nLXRlc3QtaGFzaC1rZXk
                                 |""".stripMargin)
  )
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val mobilePayeConfig: MobilePayeConfig = MobilePayeConfig(config)
  private lazy val repositoryWithoutEncrypt: P800CacheMongo = new P800CacheMongo(mongoComponent, mobilePayeConfig, encryptionEnabled = false)
  override protected val repository: P800CacheMongo = new P800CacheMongo(mongoComponent, mobilePayeConfig, encryptionEnabled = true)
  val nino: Nino = Nino("CS700100A")
  val nino1: Nino = Nino("AB000000C")
  "P800CacheMongo" should {

    "when encryption is disabled" should {

      "add new record with Nino" in {

        repositoryWithoutEncrypt.collection.drop()

        val result: Response[P800CacheHashNino] =
          repositoryWithoutEncrypt.add(P800Cache(nino)).futureValue

        result.toOption.get.nino mustBe Some(nino)

        repositoryWithoutEncrypt.collection.drop()
      }

      "find stored record with nino" in {

        repositoryWithoutEncrypt.collection.drop()

        repositoryWithoutEncrypt.add(P800Cache(nino)).futureValue

        val result: Seq[P800CacheHashNino] =
          repositoryWithoutEncrypt.selectByNino(nino).futureValue

        result.head.nino mustBe Some(nino)

        repositoryWithoutEncrypt.collection.drop()
      }

      "delete the record with nino" in {
        repositoryWithoutEncrypt.collection.drop()
        repositoryWithoutEncrypt.add(P800Cache(nino)).futureValue
        val fetchAfterAdd = repositoryWithoutEncrypt.selectByNino(nino).futureValue
        fetchAfterAdd.size mustBe 1
        repositoryWithoutEncrypt.deleteMany(nino).futureValue
        val fetchAfterDelete = repositoryWithoutEncrypt.selectByNino(nino).futureValue
        fetchAfterDelete.size mustBe 0
        repositoryWithoutEncrypt.collection.drop()
      }
    }

    "when encryption is enabled" should {

      "add new record with hashNino, with no existing records having nino in decrypted state" in {
        repository.collection.drop()
        val result: Response[P800CacheHashNino] =
          repository.add(P800Cache(nino)).futureValue
        result.toOption.get.nino mustBe None
        result.toOption.get.hashNino.get mustBe repository.hashNino(nino)
        repository.collection.drop()

      }

      "add new record with hashNino, with existing records having nino in decrypted state" in {
        repository.collection.drop()
        val result1 = repositoryWithoutEncrypt.add(P800Cache(nino1)).futureValue
        result1.toOption.get.nino mustBe Some(nino1)
        result1.toOption.get.hashNino mustBe None
        val result: Response[P800CacheHashNino] =
          repository.add(P800Cache(nino)).futureValue
        result.toOption.get.nino mustBe None
        result.toOption.get.hashNino.get mustBe repository.hashNino(nino)
        repository.collection.drop()

      }

      "find the stored record with hashNino" in {
        repository.collection.drop()
        repository.add(P800Cache(nino)).futureValue
        val result: Seq[P800CacheHashNino] =
          repository.selectByNino(nino).futureValue
        result.head.nino mustBe None
        result.head.hashNino.get mustBe repository.hashNino(nino)
        repository.collection.drop()
      }

      "delete the record with hashNino" in {
        repository.collection.drop()
        repository.add(P800Cache(nino)).futureValue
        val fetchAfterAdd = repository.selectByNino(nino).futureValue
        fetchAfterAdd.size mustBe 1
        fetchAfterAdd.head.hashNino.get mustBe repository.hashNino(nino)
        repository.deleteMany(nino).futureValue
        val fetchAfterDelete = repository.selectByNino(nino).futureValue
        fetchAfterDelete.size mustBe 0
      }
    }

  }

}
