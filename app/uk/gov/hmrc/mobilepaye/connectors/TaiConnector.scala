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

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{CoreGet, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.mobilepaye.domain._
import uk.gov.hmrc.mobilepaye.domain.tai._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class TaiConnector @Inject() (
  http:                     CoreGet,
  @Named("tai") serviceUrl: String) {

  val logger: Logger = Logger(this.getClass)

  private def url(
    nino:  Nino,
    route: String
  ) = s"$serviceUrl/tai/${nino.value}/$route"

  def getNonTaxCodeIncome(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[NonTaxCodeIncome] =
    http.GET[JsValue](url(nino, s"tax-account/$taxYear/income")).map { json =>
      (json \ "data" \ "nonTaxCodeIncomes").as[NonTaxCodeIncome]
    }

  def getTaxAccountSummary(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[TaxAccountSummary] =
    http.GET[JsValue](url(nino, s"tax-account/$taxYear/summary")).map { json =>
      (json \ "data").as[TaxAccountSummary]
    }

  def getCYPlusOneAccountSummary(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Boolean] =
    http.GET[JsValue](url(nino, s"tax-account/${taxYear + 1}/summary")).map { json =>
      (json \ "data").as[TaxAccountSummary]
      true
    } recover {
      case NonFatal(e) =>
        logger.warn(s"Couldn't retrieve tax summary for $nino with exception:${e.getMessage}")
        false
    }

  def getMatchingTaxCodeIncomes(
    nino:        Nino,
    taxYear:     Int,
    incomeType:  String,
    status:      String
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Seq[IncomeSource]] =
    http
      .GET[JsValue](url(nino, s"tax-account/year/$taxYear/income/$incomeType/status/$status"))
      .map { json =>
        (json \ "data").as[Seq[IncomeSource]]
      }
      .recover {
        case _: NotFoundException => Seq.empty[IncomeSource]
        case e => throw e
      }

  def getBenefits(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Benefits] = http.GET[JsValue](url(nino, s"tax-account/$taxYear/benefits")).map { json =>
    (json \ "data").as[Benefits]
  }

  def getTaxCodeIncomes(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Seq[TaxCodeIncome]] =
    http
      .GET[JsValue](url(nino, s"tax-account/$taxYear/income/tax-code-incomes"))
      .map { json =>
        (json \ "data").as[Seq[TaxCodeIncome]]
      }
      .recover {
        case _: NotFoundException => Seq.empty[TaxCodeIncome]
        case e => throw e
      }

  def getEmployments(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Seq[Employment]] =
    http
      .GET[JsValue](url(nino, s"employments/years/$taxYear"))
      .map { json =>
        (json \ "data" \ "employments").as[Seq[Employment]]
      }
      .recover {
        case _: NotFoundException => Seq.empty[Employment]
        case e => throw e
      }

  def getTaxCodeChangeExists(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Boolean] =
    http
      .GET[JsValue](url(nino, s"tax-account/tax-code-change/exists"))
      .map(_.as[Boolean])
      .recover {
        case _: NotFoundException => false
        case e => throw e
      }

  def getTaxCodesForYear(
    nino:        Nino,
    taxYear:     Int
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[Seq[TaxCodeRecord]] =
    http
      .GET[JsValue](url(nino, s"tax-account/$taxYear/tax-code/latest"))
      .map { json =>
        (json \ "data").as[Seq[TaxCodeRecord]]
      }
      .recover {
        case _: NotFoundException => Seq.empty[TaxCodeRecord]
        case e => throw e
      }

  def getTaxCodeChange(
    nino:        Nino
  )(implicit hc: HeaderCarrier,
    ex:          ExecutionContext
  ): Future[TaxCodeChangeDetails] =
    http
      .GET[JsValue](url(nino, s"tax-account/tax-code-change"))
      .map { json =>
        (json \ "data").as[TaxCodeChangeDetails]
      }
}
