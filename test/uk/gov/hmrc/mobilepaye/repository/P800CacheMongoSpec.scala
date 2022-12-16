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

import uk.gov.hmrc.mobilepaye.domain.P800Cache
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.serviceResponse.Response

import scala.concurrent.Await
import scala.concurrent.duration._

class P800CacheMongoSpec extends BaseSpec with DefaultPlayMongoRepositorySupport[P800Cache] {

  override lazy val repository = new P800CacheMongo(mongoComponent, appConfig)

  "P800CacheMongo" should {
    "add new record" in {

      repository.collection.drop()

      val result: Response[P800Cache] =
        Await.result(
          repository.add(
            P800Cache(nino)
          ),
          500.millis
        )

      result.right.get.nino shouldBe nino
    }

    "find stored record" in {

      repository.collection.drop()

      repository.add(
        P800Cache(nino)
      )

      val result: Seq[P800Cache] =
        Await.result(
          repository.selectByNino(
            nino
          ),
          500.millis
        )

      result.head.nino shouldBe nino

    }
  }

}
