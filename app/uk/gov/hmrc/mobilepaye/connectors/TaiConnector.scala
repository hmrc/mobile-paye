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

package uk.gov.hmrc.mobilepaye.connectors

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier}
import uk.gov.hmrc.mobilepaye.domain.tai._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaiConnector @Inject()(http: CoreGet,
                             @Named("tai") serviceUrl: String) {

  private def url(nino: Nino, route: String) = s"$serviceUrl/tai/${nino.value}/$route"

  def getPersonalDetails(nino: Nino)(implicit headerCarrier: HeaderCarrier, ex: ExecutionContext): Future[Person] = ???
  //http.GET[HttpResponse](url(nino, "person"))

  def getTaxCodeIncomes(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[TaxCodeIncome]] = ???

  def getNonTaxCodeIncome(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[NonTaxCodeIncome] = ???

  def getEmployments(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[Employment]] = ???

  def getTaxAccountSummary(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxAccountSummary] = ???

}