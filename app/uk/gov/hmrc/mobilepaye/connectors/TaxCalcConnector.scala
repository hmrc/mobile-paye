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

package uk.gov.hmrc.mobilepaye.connectors

import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.TaxYearReconciliation

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxCalcConnector @Inject() (
  httpGet:                            CoreGet,
  @Named("taxcalc") baseUrl:          String,
  @Named("with-taxcalc") withTaxCalc: Boolean
)(implicit ec:                        ExecutionContext) {

  def getTaxReconciliations(nino: Nino)(implicit hc: HeaderCarrier): Future[Option[List[TaxYearReconciliation]]] = {
    val url = baseUrl + s"/taxcalc/${nino.nino}/reconciliations"

    if (withTaxCalc) {
      httpGet.GET[Option[List[TaxYearReconciliation]]](url).recover {
        case _: Throwable => None
      }
    } else Future.successful(None)
  }
}
