/*
 * Copyright 2018 HM Revenue & Customs
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

import com.google.inject.name.Named
import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, Provides, TypeLiteral}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.api.connector.{ApiServiceLocatorConnector, ServiceLocatorConnector}
import uk.gov.hmrc.http.{CoreGet, CorePost}
import uk.gov.hmrc.mobilepaye.controllers.api.ApiAccess
import uk.gov.hmrc.mobilepaye.tasks.ServiceLocatorRegistrationTask
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}

import scala.collection.JavaConverters._

class GuiceModule(environment: Environment, configuration: Configuration) extends AbstractModule with ServicesConfig {

  override protected lazy val mode: Mode = environment.mode
  override protected lazy val runModeConfiguration: Configuration = configuration

  override def configure(): Unit = {

    bind(classOf[ServiceLocatorConnector]).to(classOf[ApiServiceLocatorConnector])
    bind(classOf[CoreGet]).to(classOf[WSHttpImpl])
    bind(classOf[CorePost]).to(classOf[WSHttpImpl])
    bind(classOf[HttpClient]).to(classOf[WSHttpImpl])
    bind(classOf[ServiceLocatorRegistrationTask]).asEagerSingleton()

    bind(classOf[ApiAccess]).toInstance(
      ApiAccess("PRIVATE", configuration.underlying.getStringList("api.access.white-list.applicationIds").asScala))

    bindConfigStringSeq("scopes")
    bind(classOf[String]).annotatedWith(named("tai")).toInstance(baseUrl("tai"))
  }

  @Provides
  @Named("appName")
  def appName: String = AppName(configuration).appName

  private def bindConfigStringSeq(path: String): Unit = {
    val configValue: Seq[String] = configuration.getStringSeq(path).getOrElse(throw new RuntimeException(s"""Config property "$path" missing"""))
    bind(new TypeLiteral[Seq[String]] {})
      .annotatedWith(named(path))
      .toInstance(configValue)
  }
}
