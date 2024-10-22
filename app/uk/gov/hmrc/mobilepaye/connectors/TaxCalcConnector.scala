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

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.TaxYearReconciliation

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.mobilepaye.domain.admin.OnlinePaymentIntegration
import uk.gov.hmrc.mobilepaye.services.admin.FeatureFlagService

@Singleton
class TaxCalcConnector @Inject() (
  httpGet:                   HttpClientV2,
  @Named("taxcalc") baseUrl: String,
  featureFlagService:        FeatureFlagService
)(implicit ec:               ExecutionContext) {

  def getTaxReconciliations(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[List[TaxYearReconciliation]]] = {
    val url = baseUrl + s"/taxcalc/${nino.nino}/reconciliations"
    featureFlagService.get(OnlinePaymentIntegration) flatMap { onlinePaymentIntegration =>
      if (onlinePaymentIntegration.isEnabled) {
        httpGet
          .get(url"$url")
          .execute[Option[List[TaxYearReconciliation]]]
          .recover {
            case _: Throwable => None
          }
      } else Future.successful(None)
    }
  }
}
