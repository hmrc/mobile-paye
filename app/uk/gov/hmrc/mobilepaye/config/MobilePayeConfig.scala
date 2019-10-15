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

package uk.gov.hmrc.mobilepaye.config
import java.nio.charset.StandardCharsets

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.mobilepaye.domain.Shuttering

trait MobilePayeControllerConfig {
  def shuttering: Shuttering
}

case class MobilePayeConfig @Inject()(configuration: Configuration) extends MobilePayeControllerConfig {
  override val shuttering: Shuttering = Shuttering(
    shuttered = configBoolean("mobilePaye.shuttering.shuttered"),
    title     = configBase64String("mobilePaye.shuttering.title"),
    message   = configBase64String("mobilePaye.shuttering.message")
  )

  private def configBoolean(path: String): Boolean = {

    val configuration2 = configuration

    configuration2.underlying.getBoolean(path)
  }
  private def configBase64String(path: String): String = {
    val encoded = configuration.underlying.getString(path)
    Base64.decode(encoded)
  }
}

object Base64 {
  private val decoder = java.util.Base64.getDecoder

  def decode(encoded: String): String = new String(decoder.decode(encoded), StandardCharsets.UTF_8)
}

