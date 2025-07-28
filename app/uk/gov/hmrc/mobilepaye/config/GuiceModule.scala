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

package uk.gov.hmrc.mobilepaye.config

import com.google.inject.name.Names.named
import com.google.inject.{AbstractModule, TypeLiteral}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector

import uk.gov.hmrc.mobilepaye.controllers.api.ApiAccess
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

class GuiceModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  val servicesConfig = new ServicesConfig(
    configuration
  )

  override def configure(): Unit = {

    bind(classOf[AuthConnector]).to(classOf[DefaultAuthConnector])
    bind(classOf[AuditConnector]).to(classOf[DefaultAuditConnector])

    bindConfigInt("controllers.confidenceLevel")
    bind(classOf[ApiAccess]).toInstance(ApiAccess("PRIVATE"))

    bindConfigStringSeq("scopes")
    bind(classOf[String]).annotatedWith(named("tai")).toInstance(servicesConfig.baseUrl("tai"))
    bind(classOf[String]).annotatedWith(named("taxcalc")).toInstance(servicesConfig.baseUrl("taxcalc"))
    bind(classOf[String])
      .annotatedWith(named("mobile-simple-assessment"))
      .toInstance(servicesConfig.baseUrl("mobile-simple-assessment"))
    bind(classOf[String])
      .annotatedWith(named("mobile-shuttering"))
      .toInstance(servicesConfig.baseUrl("mobile-shuttering"))
    bind(classOf[String]).annotatedWith(named("mobile-feedback")).toInstance(servicesConfig.baseUrl("mobile-feedback"))
    bind(classOf[String]).annotatedWith(named("citizen-details")).toInstance(servicesConfig.baseUrl("citizen-details"))
    bindConfigString("rUK.startDate", "incomeTaxComparisonPeriod.rUK.startDate")
    bindConfigString("rUK.endDate", "incomeTaxComparisonPeriod.rUK.endDate")
    bindConfigString("wales.startDate", "incomeTaxComparisonPeriod.wales.startDate")
    bindConfigString("wales.endDate", "incomeTaxComparisonPeriod.wales.endDate")
    bindConfigString("scotland.startDate", "incomeTaxComparisonPeriod.scotland.startDate")
    bindConfigString("scotland.endDate", "incomeTaxComparisonPeriod.scotland.endDate")
    bind(classOf[Boolean])
      .annotatedWith(named("p800CacheEnabled"))
      .toInstance(servicesConfig.getBoolean("p800CacheEnabled"))
    bind(classOf[Int])
      .annotatedWith(named("numberOfPreviousYearsToShowIncomeTaxHistory"))
      .toInstance(servicesConfig.getInt("numberOfPreviousYearsToShowIncomeTaxHistory"))
    bind(classOf[Int])
      .annotatedWith(named("numberOfPreviousYearsToShowPayeSummary"))
      .toInstance(servicesConfig.getInt("numberOfPreviousYearsToShowPayeSummary"))
    bind(classOf[Boolean])
      .annotatedWith(named("taxCodeChangeEnabled"))
      .toInstance(servicesConfig.getBoolean("taxCodeChangeEnabled"))
    bind(classOf[Boolean])
      .annotatedWith(named("previousEmploymentsEnabled"))
      .toInstance(servicesConfig.getBoolean("previousEmploymentsEnabled"))
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

  private def bindConfigString(
    name: String,
    path: String
  ): Unit =
    bindConstant().annotatedWith(named(name)).to(configuration.underlying.getString(path))
}
