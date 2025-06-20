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

import uk.gov.hmrc.mobilepaye.domain.P800Cache
import uk.gov.hmrc.mobilepaye.utils.{BaseSpec, PlayMongoRepositorySupport}
import uk.gov.hmrc.serviceResponse.Response

class P800CacheMongoSpec extends BaseSpec with PlayMongoRepositorySupport[P800Cache] {

  val repository: P800CacheMongo = new P800CacheMongo(mongoComponent, appConfig)

  "P800CacheMongo" should {
    "add new record" in {

      repository.collection.drop()

      val result: Response[P800Cache] =
        repository.add(P800Cache(nino)).futureValue

      result.toOption.get.nino shouldBe nino

      repository.collection.drop()
    }

    "find stored record" in {

      repository.collection.drop()

      repository.add(P800Cache(nino)).futureValue

      val result: Seq[P800Cache] =
        repository.selectByNino(nino).futureValue

      result.head.nino shouldBe nino

      repository.collection.drop()
    }
  }

}
