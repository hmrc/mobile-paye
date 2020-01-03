/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.{Configuration, Environment}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.FailoverStrategy
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{DefaultWriteResult, WriteResult}
import reactivemongo.play.json.JSONSerializationPack.Document
import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig
import uk.gov.hmrc.mobilepaye.domain.P800Cache
import uk.gov.hmrc.mongo.MongoConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MongoSpecSupport {

  protected val databaseName = "test-" + this.getClass.getSimpleName

  protected val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  implicit val mongoConnectorForTest = new MongoConnector(mongoUri)

  implicit val mongo = mongoConnectorForTest.db

  def bsonCollection(
    name:             String
  )(failoverStrategy: FailoverStrategy = mongoConnectorForTest.helper.db.failoverStrategy
  ): BSONCollection =
    mongoConnectorForTest.helper.db(name, failoverStrategy)
}

trait P800CacheMongoSetup extends MongoSpecSupport {
  private val env           = Environment.simple()
  private val configuration = Configuration.load(env)
  private val appConfig     = new MobilePayeConfig(configuration)

  class P800CacheMongoWithInsert(
    failing:      Boolean,
    doc:          Option[Document],
    errorMessage: Option[String] = Some("error")) {
    val p800CacheMongo: P800CacheMongo =
      new P800CacheMongo(new ReactiveMongoComponent {
        override def mongoConnector: MongoConnector = mongoConnectorForTest
      }, appConfig = appConfig) {

        override def insert(entity: P800Cache)(implicit ec: ExecutionContext): Future[WriteResult] =
          Future.successful(DefaultWriteResult(ok = !failing, 0, Seq.empty, Option.empty, Option.empty, Option.empty))

      }
  }

}
