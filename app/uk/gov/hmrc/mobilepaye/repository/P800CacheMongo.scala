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

import org.mongodb.scala.model.Filters.{equal, or}
import org.mongodb.scala.model.{IndexModel, IndexOptions, UpdateOptions}
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.{combine, set, setOnInsert, unset}

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig
import uk.gov.hmrc.mobilepaye.domain.{P800Cache, P800CacheHashNino}
import uk.gov.hmrc.mobilepaye.errors.MongoDBError
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.serviceResponse.ServiceResponse
import uk.gov.hmrc.crypto.{OnewayCryptoFactory, PlainText, Sha512Crypto}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class P800CacheMongo @Inject() (
  mongo: MongoComponent,
  appConfig: MobilePayeConfig,
  @Named("encryptionEnabled") encryptionEnabled: Boolean
)(implicit executionContext: ExecutionContext)
    extends PlayMongoRepository[P800CacheHashNino](
      collectionName = "p800Cache",
      mongoComponent = mongo,
      domainFormat   = P800CacheHashNino.format,
      indexes = Seq(
        IndexModel(ascending("createdAt"),
                   IndexOptions()
                     .background(false)
                     .name("createdAt")
                     .expireAfter(appConfig.mongoTtl, TimeUnit.SECONDS)
                  ),
        IndexModel(ascending("nino"),
                   IndexOptions()
                     .background(false)
                     .name("nino")
                     .unique(true)
                  ),
        IndexModel(ascending("hashNino"),
          IndexOptions()
            .name("hashNinoIdx").
            unique(true)
            .sparse(true))
      )
    ) {

  private val hasher: Sha512Crypto = OnewayCryptoFactory.sha(appConfig.ninoHashKey)

  def hashNino(nino: Nino) = hasher.hash(PlainText(nino.nino)).value

  def updateOne(p800Cache: P800Cache): Future[Boolean] = {
    if(encryptionEnabled) {
      collection
        .updateOne(
          filter = or(
            equal("hashNino", hashNino(p800Cache.nino)),
            equal("nino", p800Cache.nino.nino)
          ),
          update = combine(
            set("hashNino", hashNino(p800Cache.nino)),
            setOnInsert("createdAt", p800Cache.createdAt),
            unset("nino")
          ),
          options = UpdateOptions().upsert(true)
        )
        .toFuture()
        .map { result =>
          result.wasAcknowledged() && result.getModifiedCount > 0
        }
    } else
      Future.successful(true)
  }

  def add(p800Cache: P800Cache, withHash: Boolean = true): ServiceResponse[P800CacheHashNino] = {
    if(encryptionEnabled && withHash) {
      val hashedNino = hashNino(p800Cache.nino)
      val p800cacheUpdated = P800CacheHashNino(hashNino = Some(hashedNino))

      collection
        .insertOne(p800cacheUpdated)
        .toFuture()
        .map(_ => Right(p800cacheUpdated))
        .recover { case _ =>
          Left(MongoDBError("Unexpected error while writing a document."))
        }

    } else {
      val p800cacheUpdated = P800CacheHashNino(nino = Some(p800Cache.nino), hashNino = None)
      collection
        .insertOne(p800cacheUpdated)
        .toFuture()
        .map(_ => Right(p800cacheUpdated))
        .recover { case _ =>
          Left(MongoDBError("Unexpected error while writing a document."))
        }
    }
  }

  def deleteMany(nino: Nino): Future[Boolean] = {
    collection
      .deleteMany(
        or(
          equal("hashNino", hashNino(nino)),
          equal("nino", nino.nino)
        )
      )
      .toFuture()
      .map(_.getDeletedCount > 0)
  }

  def selectByNino(nino: Nino): Future[Seq[P800CacheHashNino]] = {
    if(encryptionEnabled) {
      val hashedNino: String = hashNino(nino)
      collection
        .find(equal("hashNino", hashedNino))
        .toFuture()
        .flatMap {
          case found if found.nonEmpty => Future.successful(found)
          case _ => collection.find(equal("nino", nino.nino)).toFuture()
        }

    } else {
      collection.find(equal("nino", nino.nino)).toFuture()
    }
  }
}
