/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.domain

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.domain.Nino

import java.time.Instant

class P800CacheSpec extends AnyWordSpec with Matchers {
  "P800CacheSpec" should {
    "serialize and deserialize P800Cache" in {
      val createdAt = Instant.parse("2026-11-18T10:30:00Z")

      val cache = P800Cache(
        nino = Nino("AA123456A"),
        createdAt = createdAt
      )

      val json = Json.toJson(cache)
      val result = json.validate[P800Cache]

      result mustBe JsSuccess(cache)
    }
  }
}
