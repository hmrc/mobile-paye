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

import org.bson.BsonDocument
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.result.DeleteResult
import play.api.libs.json.Format

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig
import uk.gov.hmrc.mobilepaye.domain.P800Cache
import uk.gov.hmrc.mobilepaye.errors.MongoDBError
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.serviceResponse.ServiceResponse

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class P800CacheMongo @Inject() (
  mongo:                     MongoComponent,
  appConfig:                 MobilePayeConfig
)(implicit executionContext: ExecutionContext)
    extends PlayMongoRepository[P800Cache](
      collectionName = "p800Cache",
      mongoComponent = mongo,
      domainFormat   = P800Cache.format,
      indexes = Seq(
        IndexModel(ascending("createdAt"),
                   IndexOptions()
                     .background(false)
                     .name("createdAt")
                     .expireAfter(appConfig.mongoTtl, TimeUnit.SECONDS)),
        IndexModel(ascending("nino"),
                   IndexOptions()
                     .background(false)
                     .name("nino")
                     .unique(true))
      )
    ) {

  def add(p800Cache: P800Cache): ServiceResponse[P800Cache] =
    collection
      .insertOne(p800Cache)
      .toFuture()
      .map(_ => Right(p800Cache))
      .recover {
        case _ => Left(MongoDBError("Unexpected error while writing a document."))
      }

  def selectByNino(nino: Nino): Future[Seq[P800Cache]] =
    collection.find(equal("nino", nino.nino)).toFuture()
}
