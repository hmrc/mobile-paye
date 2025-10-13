/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class EnumUtilsSpec extends AnyWordSpec with Matchers {

  object TestEnum extends Enumeration {
    val Alpha, Beta, Gamma = Value
  }

  import EnumUtils.*

  "EnumUtils" should {

    "successfully read a valid enum string from JSON" in {
      val json = JsString("Alpha")
      val result = json.validate[TestEnum.Value](enumReads(TestEnum))
      result mustBe JsSuccess(TestEnum.Alpha)
    }

    "fail to read an invalid enum string from JSON with appropriate error" in {
      val json = JsString("InvalidValue")
      val result: JsResult[TestEnum.Value] = json.validate[TestEnum.Value](enumReads(TestEnum))

      result mustBe a[JsError]
      result.asEither.swap.getOrElse(Nil).head._2.head.message must include("Expected an enumeration of type")
      result.asEither.swap.getOrElse(Nil).head._2.head.message must include("InvalidValue")
    }

    "fail to read a non-string JSON value" in {
      val json = JsNumber(42)
      val result = json.validate[TestEnum.Value](enumReads(TestEnum))

      result mustBe JsError("String value is expected")
    }

    "write an enum value to JSON as a string" in {
      val json = Json.toJson(TestEnum.Beta)
      json mustBe JsString("Beta")
    }

    "support round-trip format (write then read)" in {
      val json = Json.toJson(TestEnum.Gamma)
      val parsed = json.validate[TestEnum.Value](enumReads(TestEnum))

      parsed mustBe JsSuccess(TestEnum.Gamma)
    }
  }

  "InvalidEnumException" should {

    "generate a descriptive message" in {
      val ex = new InvalidEnumException("TestEnum", "Unknown")
      ex.getMessage must include("TestEnum")
      ex.getMessage must include("Unknown")
    }
  }
}
