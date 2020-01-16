/*
 * Copyright 2020 HM Revenue & Customs
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

import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, TypeLiteral}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{CoreGet, CorePost}
import uk.gov.hmrc.mobilepaye.controllers.api.ApiAccess
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.collection.JavaConverters._

class GuiceModule(
  environment:   Environment,
  configuration: Configuration)
    extends AbstractModule {

  val servicesConfig = new ServicesConfig(
    configuration,
    new RunMode(configuration, environment.mode)
  )

  override def configure(): Unit = {

    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    bind(classOf[CoreGet]).to(classOf[WSHttpImpl])
    bind(classOf[CorePost]).to(classOf[WSHttpImpl])
    bind(classOf[HttpClient]).to(classOf[WSHttpImpl])

    bindConfigInt("controllers.confidenceLevel")
    bind(classOf[ApiAccess]).toInstance(
      ApiAccess(
        "PRIVATE",
        configuration.underlying
          .getStringList("api.access.white-list.applicationIds")
          .asScala
      )
    )

    bindConfigStringSeq("scopes")
    bind(classOf[String]).annotatedWith(named("tai")).toInstance(servicesConfig.baseUrl("tai"))
    bind(classOf[String]).annotatedWith(named("taxcalc")).toInstance(servicesConfig.baseUrl("taxcalc"))
    bind(classOf[Boolean])
      .annotatedWith(named("with-taxcalc"))
      .toInstance(servicesConfig.getBoolean("mobilePaye.feature-flags.taxcalc"))
    bind(classOf[String])
      .annotatedWith(named("mobile-shuttering"))
      .toInstance(servicesConfig.baseUrl("mobile-shuttering"))
  }

  private def bindConfigStringSeq(path: String): Unit = {
    val configValue: Seq[String] = configuration
      .getOptional[Seq[String]](path)
      .getOrElse(
        throw new RuntimeException(s"""Config property "$path" missing""")
      )
    bind(new TypeLiteral[Seq[String]] {})
      .annotatedWith(named(path))
      .toInstance(configValue)
  }

  private def bindConfigInt(path: String): Unit =
    bindConstant()
      .annotatedWith(named(path))
      .to(configuration.underlying.getInt(path))
}
