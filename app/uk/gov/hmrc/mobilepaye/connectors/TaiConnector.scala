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
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaiConnector @Inject()(http: CoreGet,
                             @Named("tai") serviceUrl: String) {

  private def url(nino: Nino, route: String) = s"$serviceUrl/tai/${nino.value}/$route"

  def getPerson(nino: Nino)(implicit headerCarrier: HeaderCarrier, ex: ExecutionContext): Future[Person] = {
    http.GET[JsValue](url(nino, "person")).map {
      json => (json \ "data").as[Person]
    }
  }

  def getTaxCodeIncomes(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[TaxCodeIncome]] = {
    http.GET[JsValue](url(nino, s"tax-account/${TaxYear.current.currentYear}/income/tax-code-incomes")).map {
      json => (json \ "data").as[Seq[TaxCodeIncome]]
    }.recover {
      case _: NotFoundException => Seq.empty[TaxCodeIncome]
      case e => throw e
    }
  }

  def getNonTaxCodeIncome(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[NonTaxCodeIncome] = {
    http.GET[JsValue](url(nino, s"tax-account/${TaxYear.current.currentYear}/income")).map {
      json => (json \ "data" \ "nonTaxCodeIncomes").as[NonTaxCodeIncome]
    }
  }

  def getEmployments(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[Employment]] = {
    http.GET[JsValue](url(nino, s"employments/years/${TaxYear.current.currentYear}")).map {
      json => (json \ "data").as[Seq[Employment]]
    }.recover {
      case _: NotFoundException => Seq.empty[Employment]
      case e => throw e
    }
  }

  def getTaxAccountSummary(nino: Nino)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxAccountSummary] = {
    http.GET[JsValue](url(nino, s"tax-account/${TaxYear.current.currentYear}/summary")).map {
      json => (json \ "data").as[TaxAccountSummary]
    }
  }

}