/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig
import uk.gov.hmrc.mobilepaye.domain.P800Cache
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.serviceResponse.ServiceResponse

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class P800CacheMongo @Inject() (
  mongo:     ReactiveMongoComponent,
  appConfig: MobilePayeConfig
)(
  implicit executionContext: ExecutionContext)
    extends ReactiveRepository[P800Cache, BSONObjectID](
      collectionName = "p800Cache",
      mongo          = mongo.mongoConnector.db,
      domainFormat   = P800Cache.format
    )
    with MongoHelper {

  override def indexes: Seq[Index] =
    Seq(
      Index(
        Seq("createdAt" -> IndexType.Ascending),
        name    = Some("createdAt"),
        sparse  = false,
        options = BSONDocument("expireAfterSeconds" -> appConfig.mongoTtl)
      ),
      Index(Seq("nino" -> IndexType.Ascending), Some("nino"), unique = true)
    )

  def add(p800Cache: P800Cache): ServiceResponse[P800Cache] =
    insert(p800Cache)
      .map(
        result =>
          handleWriteResult[P800Cache](
            result,
            p800Cache
          )
      )

  def selectByNino(nino: Nino): Future[List[P800Cache]] =
    find("nino" -> nino)
}
