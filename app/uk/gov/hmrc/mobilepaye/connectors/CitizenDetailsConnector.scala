/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.connectors

import com.google.inject.{Inject, Singleton}
import com.google.inject.name.Named
import play.api.Logger
import play.api.http.Status.{LOCKED, NOT_FOUND}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.mobilepaye.domain.citizendetails.Person
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.HttpReads.Implicits.*

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsConnector @Inject() (@Named("citizen-details") citizenDetailsConnectorUrl: String, http: HttpClientV2) {

  val logger: Logger = Logger(this.getClass)

  def getPerson(
    nino: Nino
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Person] =
    http
      .get(url"$citizenDetailsConnectorUrl/citizen-details/$nino/designatory-details/basic")
      .execute[Person]
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == LOCKED =>
          logger.info("Person details are hidden")
          throw UpstreamErrorResponse(e.getMessage(), LOCKED)
        case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND =>
          logger.info(s"No details found for nino '$nino'")
          throw UpstreamErrorResponse(e.getMessage(), NOT_FOUND)
      }
}
