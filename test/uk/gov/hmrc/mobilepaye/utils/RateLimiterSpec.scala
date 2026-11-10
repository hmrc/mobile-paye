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

package uk.gov.hmrc.mobilepaye.utils

class RateLimiterSpec extends BaseSpec {

  "RateLimiter" should {
    "create NoOp instance when disabled" in {
      val rateLimiter = RateLimiter(false, 100)
      rateLimiter shouldBe NoOpRateLimiter
    }

    "create real instance when disabled" in {
      val rateLimiter = RateLimiter(true, 100)
      rateLimiter shouldBe a [ThrottlingRateLimiter]
    }
  }

  "mayProceed" should {

    val maxTps = 0.1

    "always return true for NoOpRateLimiter" in {
      val rateLimiter = RateLimiter(false, maxTps)
      rateLimiter.mayProceed() shouldBe true
      rateLimiter.mayProceed() shouldBe true
    }
    "return false for ThrottlingRateLimiter when limit is exceeded" in {
      val rateLimiter = RateLimiter(true, maxTps)
      rateLimiter.mayProceed() shouldBe true
      rateLimiter.mayProceed() shouldBe false
    }
  }

}
