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

package uk.gov.hmrc.mobilepaye.repository

import com.google.inject.Inject
import play.api.Logger

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RunOnStartup @Inject() (p800CacheMongo: P800CacheMongo)(implicit executionContext: ExecutionContext) {
  val logger: Logger = Logger(this.getClass)

  val resetCache: Future[Unit] = for {
    deleteCache <- p800CacheMongo.deleteCacheRecords()
  } yield (logger.info(
    s"\n====================== P800 CACHE DELETED ======================\n\nSuccess = ${deleteCache.wasAcknowledged()}\nRecords removed = ${deleteCache.getDeletedCount}\n\n========================================================================================"
  ))

  resetCache.recover {
    case e => logger.warn("Resetting P800 cache failed: " + e)
  }

}
