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

package uk.gov.hmrc.mobilepaye.services

import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, InternalServerException, UnauthorizedException}
import uk.gov.hmrc.mobilepaye.connectors.{TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.{IncomeSource, IncomeTaxYear, MobilePayeResponse, OtherBenefits, P800Cache}
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class IncomeTaxHistoryServiceSpec extends BaseSpec {

  val mockTaiConnector: TaiConnector = mock[TaiConnector]

  val service = new IncomeTaxHistoryService(mockTaiConnector, 5)

  def mockTaxCodeIncomes(
    f:       Future[Seq[TaxCodeIncome]],
    taxYear: Int
  ) =
    (mockTaiConnector
      .getTaxCodeIncomes(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, taxYear, *, *)
      .returning(f)

  def mockEmployments(
    f:       Future[Seq[Employment]],
    taxYear: Int
  ) =
    (mockTaiConnector
      .getEmployments(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, taxYear, *, *)
      .returning(f)

  "getIncomeTaxHistory" should {
    "return full IncomeTaxHistory when all data is available" in {

      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2022)
      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2021)
      mockTaxCodeIncomes(Future successful Seq.empty, 2020)
      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2019)
      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2018)
      mockEmployments(Future successful Seq(taiEmployment, taiEmployment2), 2022)
      mockEmployments(Future successful Seq(taiEmployment, taiEmployment2), 2021)
      mockEmployments(Future successful Seq(taiEmployment, taiEmployment2), 2019)
      mockEmployments(Future successful Seq(taiEmployment, taiEmployment2), 2018)

      val test = await(service.getIncomeTaxHistoryYearsList(nino))
      println(Json.prettyPrint(Json.toJson(test)))

    }

    "return no income data when none available" in {

      mockTaxCodeIncomes(Future successful Seq.empty, 2022)
      mockTaxCodeIncomes(Future successful Seq.empty, 2021)
      mockTaxCodeIncomes(Future successful Seq.empty, 2020)
      mockTaxCodeIncomes(Future successful Seq.empty, 2019)
      mockTaxCodeIncomes(Future successful Seq.empty, 2018)

      val test = await(service.getIncomeTaxHistoryYearsList(nino))
      println(Json.prettyPrint(Json.toJson(test)))

    }

  }

}
