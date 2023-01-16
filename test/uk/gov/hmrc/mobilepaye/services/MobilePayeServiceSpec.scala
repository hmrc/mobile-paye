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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, InternalServerException, UnauthorizedException}
import uk.gov.hmrc.mobilepaye.connectors.{TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.{IncomeSource, MobilePayeResponse, P800Cache}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class MobilePayeServiceSpec extends BaseSpec with DefaultPlayMongoRepositorySupport[P800Cache] {

  override lazy val repository = new P800CacheMongo(mongoComponent, appConfig)

  val mockTaiConnector:     TaiConnector      = mock[TaiConnector]
  val mockTaxCalcConnector: TaxCalcConnector  = mock[TaxCalcConnector]
  val p800CacheMongo:       P800CacheMongo    = repository
  val dateFormatter:        DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd\'T\'HH:mm:ss")
  val inactiveDate:         String            = "2020-02-01T00:00:00"
  val activeStartDate:      String            = LocalDateTime.now(ZoneId.of("Europe/London")).minusDays(10).format(dateFormatter)
  val activeEndDate:        String            = LocalDateTime.now(ZoneId.of("Europe/London")).plusDays(10).format(dateFormatter)

  val service = new MobilePayeService(mockTaiConnector,
                                      mockTaxCalcConnector,
                                      p800CacheMongo,
                                      inactiveDate,
                                      inactiveDate,
                                      inactiveDate,
                                      inactiveDate,
                                      inactiveDate,
                                      inactiveDate,
                                      true)

  def mockMatchingTaxCode(f: Future[Seq[IncomeSource]]) =
    (mockTaiConnector
      .getMatchingTaxCodeIncomes(_: Nino, _: Int, _: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returning(f)

  def mockNonTaxCodeIncomes(f: Future[NonTaxCodeIncome]) =
    (mockTaiConnector
      .getNonTaxCodeIncome(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(f)

  def mockTaxAccountSummary(f: Future[TaxAccountSummary]) =
    (mockTaiConnector
      .getTaxAccountSummary(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(f)

  def mockP800Summary() =
    (mockTaxCalcConnector
      .getTaxReconciliations(_: Nino)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(None))

  def mockCYPlusOneAccountSummary(f: Future[Boolean]) =
    (mockTaiConnector
      .getCYPlusOneAccountSummary(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(f)

  "getMobilePayeResponse" should {
    "return full MobilePayeResponse when all data is available" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSource))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse
    }

    "return full MobilePayeResponse with tax comparison link during Welsh active period" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSourceWelsh))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockCYPlusOneAccountSummary(Future successful true)
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          inactiveDate,
                                          inactiveDate,
                                          activeStartDate,
                                          activeEndDate,
                                          inactiveDate,
                                          inactiveDate,
                                          true)

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponseWithCY1Link.copy(taxCodeLocation = Some("Welsh"),
                                                             employments = Some(welshEmployments))
    }

    "return full MobilePayeResponse with tax comparison link during UK active period" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSourceUK))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockCYPlusOneAccountSummary(Future successful true)
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          activeStartDate,
                                          activeEndDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          true)

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponseWithCY1Link.copy(taxCodeLocation = Some("rUK"),
                                                             employments = Some(ukEmployments))
    }

    "return full MobilePayeResponse with tax comparison link during Scottish active period" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSource))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockCYPlusOneAccountSummary(Future successful true)
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          activeStartDate,
                                          activeEndDate,
                                          true)

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponseWithCY1Link
    }

    "return full MobilePayeResponse with no tax comparison link if not in active period" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSourceWelsh))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          activeStartDate,
                                          activeEndDate,
                                          inactiveDate,
                                          inactiveDate,
                                          activeStartDate,
                                          activeEndDate,
                                          true)

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(employments = Some(welshEmployments))
    }

    "return MobilePayeResponse with no untaxed interest" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSource))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = Some(Seq(otherIncome)))
    }

    "return MobilePayeResponse with no employments when employment data is missing" in {
      mockMatchingTaxCode(Future.successful(Seq.empty[IncomeSource]))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(employments = None)
    }

    "return MobilePayeResponse with no pensions when pension data is missing" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSource))
      mockMatchingTaxCode(Future.successful(Seq.empty[IncomeSource]))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return MobilePayeResponse with no otherIncomes when OtherIncome data is missing" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSource))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(
        Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest.copy(otherNonTaxCodeIncomes = Nil))
      )
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
    }

    "return MobilePayeResponse with correct employment latestPayments" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSource))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result         = await(service.getMobilePayeResponse(nino, currentTaxYear))
      val latestPayment1 = result.employments.get.head.latestPayment.get
      val latestPayment2 = result.employments.get.last.latestPayment

      latestPayment1.amount                            shouldBe 50
      latestPayment1.taxAmount                         shouldBe 5
      latestPayment1.nationalInsuranceAmount           shouldBe 2
      latestPayment1.amountYearToDate                  shouldBe 100
      latestPayment1.taxAmountYearToDate               shouldBe 20
      latestPayment1.nationalInsuranceAmountYearToDate shouldBe 10
      latestPayment1.futurePayment                     shouldBe false
      latestPayment1.link                              shouldBe "/check-income-tax/your-income-calculation-details/3"
      latestPayment2                                   shouldBe None
    }

    "return MobilePayeResponse with correct employment latestPayments and previousPayments" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSource2))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result         = await(service.getMobilePayeResponse(nino, currentTaxYear))
      val latestPayment1 = result.employments.get.head.latestPayment.get

      latestPayment1.amount                            shouldBe 50
      latestPayment1.taxAmount                         shouldBe 5
      latestPayment1.nationalInsuranceAmount           shouldBe 2
      latestPayment1.amountYearToDate                  shouldBe 100
      latestPayment1.taxAmountYearToDate               shouldBe 20
      latestPayment1.nationalInsuranceAmountYearToDate shouldBe 10
      latestPayment1.futurePayment                     shouldBe true
      latestPayment1.link                              shouldBe "/check-income-tax/your-income-calculation-details/3"
      result.employments.get.head.payments.get.size    shouldBe 3
    }

    "return MobilePayeResponse with no payments node for employment with no payments" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSourceNoPayments))
      mockMatchingTaxCode(Future.successful(pensionIncomeSourceNoPension))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      val payments = result.employments.get.head.payments

      payments shouldBe None
    }

    "return MobilePayeResponse with no latestPayment for pension" in {
      mockMatchingTaxCode(Future.successful(employmentIncomeSource))
      mockMatchingTaxCode(Future.successful(pensionIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result.pensions.get.head.latestPayment shouldBe None
    }

    "throw UnauthorizedException when receiving UnauthorizedException from taiConnector" in {
      mockMatchingTaxCode(Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(service.getMobilePayeResponse(nino, currentTaxYear))
      }
    }

    "throw ForbiddenException when receiving ForbiddenException from taiConnector" in {
      mockMatchingTaxCode(Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException] {
        await(service.getMobilePayeResponse(nino, currentTaxYear))
      }
    }

    "throw InternalServerError when receiving InternalServerError from taiConnector" in {
      mockMatchingTaxCode(Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(service.getMobilePayeResponse(nino, currentTaxYear))
      }
    }

    "return an empty MobilePayeResponse when an exception is thrown that contains 'no employments recorded for current tax year'" in {
      mockMatchingTaxCode(Future.failed(new Exception("no employments recorded for current tax year")))

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe MobilePayeResponse.empty
    }

    "return an empty MobilePayeResponse when an exception is thrown from NPS that contains 'cannot complete a coding calculation without a primary employment'" in {
      mockMatchingTaxCode(
        Future.failed(new Exception("cannot complete a coding calculation without a primary employment"))
      )

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe MobilePayeResponse.empty
    }

    "return an empty MobilePayeResponse when an exception is thrown from NPS that contains 'no employments recorded for this individual'" in {
      mockMatchingTaxCode(Future.failed(new Exception("no employments recorded for this individual")))

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe MobilePayeResponse.empty
    }

  }

}
