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

package uk.gov.hmrc.mobilepaye.domain.admin

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.*
import play.api.mvc.PathBindable


class FeatureFlagSpec extends PlaySpec {

  "FeatureFlag JSON serialization" should {

    "serialize FeatureFlag to JSON correctly" in {
      val flag = FeatureFlag(OnlinePaymentIntegration, isEnabled = true, description = Some("Some desc"))
      val json = Json.toJson(flag)

      (json \ "name").as[String] mustBe "online-payment-integration"
      (json \ "isEnabled").as[Boolean] mustBe true
      (json \ "description").asOpt[String] mustBe Some("Some desc")
    }

    "deserialize valid JSON to FeatureFlag correctly" in {
      val json = Json.parse("""
          |{
          | "name": "online-payment-integration",
          | "isEnabled": true,
          | "description": "Some desc"
          |}
          |""".stripMargin)

      val result = json.validate[FeatureFlag]
      result.isSuccess mustBe true
      result.get.name mustBe OnlinePaymentIntegration
      result.get.isEnabled mustBe true
      result.get.description mustBe Some("Some desc")
    }

    "fail to deserialize unknown FeatureFlagName" in {
      val json = Json.parse("""
          |{
          | "name": "unknown-flag",
          | "isEnabled": false
          |}
          |""".stripMargin)

      val result = json.validate[FeatureFlag]
      result.isError mustBe true
    }
  }

  "FeatureFlagName PathBindable" should {

    val bindable = implicitly[PathBindable[FeatureFlagName]]

    "bind a valid feature flag string to FeatureFlagName" in {
      val result = bindable.bind("name", "online-payment-integration")
      result mustBe Right(OnlinePaymentIntegration)
    }

    "unbind FeatureFlagName to correct string" in {
      val result = bindable.unbind("name", OnlinePaymentIntegration)
      result mustBe "online-payment-integration"
    }

    "fail to bind an unknown feature flag string" in {
      val result = bindable.bind("name", "invalid-flag")
      result mustBe Left("The feature flag `invalid-flag` does not exist")
    }
  }

  "FeatureFlagName description" should {

    "return the correct description for OnlinePaymentIntegration" in {
      OnlinePaymentIntegration.description mustBe Some("Enable/disable online payment integration")
    }

  }
}
