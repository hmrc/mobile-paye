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
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, InternalServerException, NotFoundException, UnauthorizedException}
import uk.gov.hmrc.mobilepaye.connectors.{TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.{IncomeSource, IncomeTaxYear, MobilePayeResponse, OtherBenefits, P800Cache}
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneId}
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
      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2020)
      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2019)
      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2018)
      mockEmployments(Future successful Seq(taiEmployment(2022), taiEmployment2), 2022)
      mockEmployments(Future successful Seq(taiEmployment(2021), taiEmployment2), 2021)
      mockEmployments(Future successful Seq(taiEmployment(2020), taiEmployment2), 2020)
      mockEmployments(Future successful Seq(taiEmployment(2019), taiEmployment2), 2019)
      mockEmployments(Future successful Seq(taiEmployment(2018), taiEmployment2), 2018)

      val response: Seq[IncomeTaxYear] = await(service.getIncomeTaxHistoryYearsList(nino))
      response.size                                shouldBe 5
      response.head.taxYear.startYear              shouldBe 2022
      response.head.incomes.get.size               shouldBe 2
      response.head.incomes.get.head.name          shouldBe "TESCO"
      response.head.incomes.get.head.startDate     shouldBe LocalDate.now().minusYears(4)
      response.head.incomes.get.head.amount.get    shouldBe 100
      response.head.incomes.get.head.taxAmount.get shouldBe 20
      response.head.incomes.get.head.taxCode.get   shouldBe "S1150L"
      response.head.incomes.get.head.isPension     shouldBe false

    }

    "return no income data when none available and do not call employment endpoint" in {

      mockTaxCodeIncomes(Future failed new NotFoundException("Not Found"), 2022)
      mockTaxCodeIncomes(Future failed new NotFoundException("Not Found"), 2021)
      mockTaxCodeIncomes(Future failed new NotFoundException("Not Found"), 2020)
      mockTaxCodeIncomes(Future failed new NotFoundException("Not Found"), 2019)
      mockTaxCodeIncomes(Future failed new NotFoundException("Not Found"), 2018)

      val response = await(service.getIncomeTaxHistoryYearsList(nino))
      response.size                   shouldBe 5
      response.head.incomes.isDefined shouldBe false
      response(1).incomes.isDefined   shouldBe false
      response(2).incomes.isDefined   shouldBe false
      response(3).incomes.isDefined   shouldBe false
      response(4).incomes.isDefined   shouldBe false

    }

    "return no income data when call fails for that year" in {

      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2022)
      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2021)
      mockTaxCodeIncomes(Future failed new Exception("Unexpected Exception"), 2020)
      mockTaxCodeIncomes(Future failed new Exception("Unexpected Exception"), 2019)
      mockTaxCodeIncomes(Future failed new Exception("Unexpected Exception"), 2018)
      mockEmployments(Future successful Seq(taiEmployment(2022), taiEmployment2), 2022)
      mockEmployments(Future successful Seq(taiEmployment(2021), taiEmployment2), 2021)

      val response = await(service.getIncomeTaxHistoryYearsList(nino))
      response.size                   shouldBe 5
      response.head.incomes.isDefined shouldBe true
      response(1).incomes.isDefined   shouldBe true
      response(2).incomes.isDefined   shouldBe false
      response(3).incomes.isDefined   shouldBe false
      response(4).incomes.isDefined   shouldBe false

    }

    "return number of years set by config" in {

      val service = new IncomeTaxHistoryService(mockTaiConnector, 2)

      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2022)
      mockTaxCodeIncomes(Future successful Seq(taxCodeIncome, taxCodeIncome2, taxCodeIncome3), 2021)
      mockEmployments(Future successful Seq(taiEmployment(2022), taiEmployment2), 2022)
      mockEmployments(Future successful Seq(taiEmployment(2021), taiEmployment2), 2021)

      val response = await(service.getIncomeTaxHistoryYearsList(nino))
      response.size                   shouldBe 2
      response.head.incomes.isDefined shouldBe true
      response(1).incomes.isDefined   shouldBe true

    }

  }

}
