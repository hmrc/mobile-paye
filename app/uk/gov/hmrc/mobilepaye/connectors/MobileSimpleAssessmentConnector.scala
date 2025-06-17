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

package uk.gov.hmrc.mobilepaye.connectors

import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, StringContextOps}
import uk.gov.hmrc.mobilepaye.domain.admin.OnlinePaymentIntegration
import uk.gov.hmrc.mobilepaye.services.admin.FeatureFlagService
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.MobileSimpleAssessmentResponse
import uk.gov.hmrc.mobilepaye.domain.types.JourneyId

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

case class MobileSimpleAssessmentConnector @Inject() (
  httpGet: HttpClientV2,
  @Named("mobile-simple-assessment") baseUrl: String,
  featureFlagService: FeatureFlagService
)(implicit ec: ExecutionContext) {

  val logger: Logger = Logger(this.getClass)

  def getSimpleAssessmentLiabilities(
    journeyId: JourneyId
  )(implicit hc: HeaderCarrier): Future[Option[MobileSimpleAssessmentResponse]] = {
    val url = baseUrl + s"/liabilities?journeyId=${journeyId.value.toString}"
    featureFlagService.get(OnlinePaymentIntegration) flatMap { onlinePaymentIntegration =>
      if (onlinePaymentIntegration.isEnabled) {
        httpGet
          .get(url"$url")
          .execute[Option[MobileSimpleAssessmentResponse]]
          .recover {
            case _: NotFoundException => None
            case e: Throwable => {
              logger.warn(s"Call to mobile-simple-assessment failed. Reason: ${e.getCause} ${e.getMessage}")
              None
            }
          }
      } else Future.successful(None)
    }
  }
}
