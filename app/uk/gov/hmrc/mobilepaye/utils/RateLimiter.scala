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

import com.google.common.util.concurrent.RateLimiter as GoogleRateLimiter

import java.time.Duration
import scala.concurrent.Future

case class RateLimitedException() extends RuntimeException

trait RateLimiter {
  def mayProceed(): Boolean
}

object RateLimiter {
  def apply(enabled: Boolean, maxTps: Double): RateLimiter = {
    if enabled
    then ThrottlingRateLimiter(GoogleRateLimiter.create(maxTps))
    else NoOpRateLimiter
  }
}

private[utils] object NoOpRateLimiter extends RateLimiter {
  def mayProceed(): Boolean = true
}

private[utils] case class ThrottlingRateLimiter(impl: GoogleRateLimiter, permits: Int = 1, timeout: Duration = Duration.ZERO) extends RateLimiter {
  def mayProceed(): Boolean = impl.tryAcquire(permits, timeout)
}

object Throttle {
  def withThrottle[A](fun: => Future[A])(using rateLimiter: RateLimiter): Future[A] = {
    if rateLimiter.mayProceed() then fun else Future.failed(RateLimitedException())
  }
}
