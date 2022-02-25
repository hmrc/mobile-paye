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

import org.scalatest.{Matchers, WordSpec}
import org.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.P800Cache
import uk.gov.hmrc.serviceResponse.Response

import scala.concurrent.Await
import scala.concurrent.duration._

class P800CacheMongoSpec
    extends WordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with MockitoSugar
    with P800CacheMongoSetup {
  val nino: Nino = Nino("CS700100A")

  "P800CacheMongo" should {

    "add new record" in new P800CacheMongoWithInsert(
      false,
      Some(
        Json
          .toJson(
            P800Cache(nino)
          )
          .as[JsObject]
      )
    ) {

      val result: Response[P800Cache] =
        Await.result(
          p800CacheMongo.add(
            P800Cache(nino)
          ),
          500.millis
        )

      result.right.get.nino shouldBe nino
    }

    "add but Error" in new P800CacheMongoWithInsert(
      true,
      Some(
        Json
          .toJson(
            P800Cache(nino)
          )
          .as[JsObject]
      )
    ) {

      val result: Response[P800Cache] =
        Await.result(
          p800CacheMongo.add(
            P800Cache(nino)
          ),
          500.millis
        )

      result.left.get.message shouldBe "Unexpected error while writing a document."

    }
  }

}
