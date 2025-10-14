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

import com.fasterxml.jackson.core.JsonParseException
import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import play.api.Logger
import play.api.libs.json.{JsResultException, JsValue}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, StringContextOps}
import uk.gov.hmrc.mobilepaye.domain.*
import uk.gov.hmrc.mobilepaye.domain.tai.*
import uk.gov.hmrc.http.HttpReads.Implicits.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class TaiConnector @Inject() (http: HttpClientV2, @Named("tai") serviceUrl: String) {

  val logger: Logger = Logger(this.getClass)

  private def url(
    nino: Nino,
    route: String
  ) = s"$serviceUrl/tai/${nino.value}/$route"

  def getNonTaxCodeIncome(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[NonTaxCodeIncome] =
    http
      .get(url"${url(nino, s"tax-account/$taxYear/income")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data" \ "nonTaxCodeIncomes").as[NonTaxCodeIncome]
      }

  def getTaxAccountSummary(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxAccountSummary] =
    http
      .get(url"${url(nino, s"tax-account/$taxYear/summary")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data").as[TaxAccountSummary]
      }

  def getCYPlusOneAccountSummary(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Boolean] =
    http
      .get(url"${url(nino, s"tax-account/${taxYear + 1}/summary")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data").as[TaxAccountSummary]
        true
      } recover { case NonFatal(e) =>
      logger.warn(s"Couldn't retrieve tax summary for $nino with exception:${e.getMessage}")
      false
    }

  def getMatchingTaxCodeIncomes(
    nino: Nino,
    taxYear: Int,
    incomeType: String,
    status: String
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[IncomeSource]] =
    http
      .get(url"${url(nino, s"tax-account/year/$taxYear/income/$incomeType/status/$status")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data").as[Seq[IncomeSource]]
      }
      .recover {
        case jex: JsonParseException =>
          throw new JsonParseException(s"GET of tax-account/year/$taxYear/income/$incomeType/status/$status Failed with ${jex.getMessage}")
        case _: NotFoundException => Seq.empty[IncomeSource]
        case _: JsResultException =>
          throw NotFoundException(s"GET of tax-account/year/$taxYear/income/$incomeType/status/$status Failed with 404")
        case e => throw e
      }

  def getBenefits(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Benefits] =
    http
      .get(url"${url(nino, s"tax-account/$taxYear/benefits")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data").as[Benefits]
      }

  def getTaxCodeIncomes(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[TaxCodeIncome]] =
    http
      .get(url"${url(nino, s"tax-account/$taxYear/income/tax-code-incomes")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data").as[Seq[TaxCodeIncome]]
      }
      .recover { case e =>
        Seq.empty[TaxCodeIncome]
      }

  def getEmployments(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[Employment]] = {
    for {
      employments    <- getEmploymentsOnly(nino, taxYear)
      annualAccounts <- getAnnualAccounts(nino, taxYear)
    } yield {
      employments.map { employment =>
        val accounts = annualAccounts.filter(_.sequenceNumber == employment.sequenceNumber)
        employment.copy(annualAccounts = accounts)

      }
    }
  }

  def getEmploymentsOnly(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[Employment]] =
    http
      .get(url"${url(nino, s"employments-only/years/$taxYear")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data" \ "employments").as[Seq[Employment]]
      }
      .recover {
        case jex: JsonParseException => throw new JsonParseException(s"GET of employments-only/years/$taxYear Failed with ${jex.getMessage}")
        case _: NotFoundException    => Seq.empty[Employment]
        case _: JsResultException    => Seq.empty[Employment]
        case e                       => throw e
      }

  def getAnnualAccounts(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[AnnualAccount]] = {
    http
      .get(url"${url(nino, s"rti-payments/years/$taxYear")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data").as[Seq[AnnualAccount]]
      }
      .recover {
        case jex: JsonParseException => throw new JsonParseException(s"GET of rti-payments/years/$taxYear Failed with ${jex.getMessage}")
        case _: NotFoundException    => Seq.empty[AnnualAccount]
        case _: JsResultException    => Seq.empty[AnnualAccount]
        case e                       => throw e
      }
  }

  def getTaxCodeChangeExists(
    nino: Nino
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Boolean] =
    http
      .get(url"${url(nino, s"tax-account/tax-code-change/exists")}")
      .execute[JsValue]
      .map(_.as[Boolean])
      .recover {
        case jex: JsonParseException => throw new JsonParseException(s"GET of tax-account/tax-code-change/exists Failed with ${jex.getMessage}")
        case _: NotFoundException    => false
        case e                       => throw e
      }

  def getTaxCodesForYear(
    nino: Nino,
    taxYear: Int
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Seq[TaxCodeRecord]] =
    http
      .get(url"${url(nino, s"tax-account/$taxYear/tax-code/latest")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data").as[Seq[TaxCodeRecord]]
      }
      .recover {
        case _: NotFoundException => Seq.empty[TaxCodeRecord]
        case _: JsResultException => Seq.empty[TaxCodeRecord]
        case e                    => throw e
      }

  def getTaxCodeChange(
    nino: Nino
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[TaxCodeChangeDetails] =
    http
      .get(url"${url(nino, s"tax-account/tax-code-change")}")
      .execute[JsValue]
      .map { json =>
        (json \ "data").as[TaxCodeChangeDetails]
      }
      .recover { case _: JsResultException =>
        TaxCodeChangeDetails(Seq.empty[TaxCodeRecord], Seq.empty[TaxCodeRecord])
      }
}
