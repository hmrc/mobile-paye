/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, NotFoundException}
import uk.gov.hmrc.mobilepaye.connectors.{TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.tai.{Benefits, Ceased, Employment, NonTaxCodeIncome, TaxAccountSummary, TaxCodeRecord}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.time.TaxYear

import scala.concurrent.{ExecutionContext, Future}

class PreviousYearSummaryServiceSpec extends BaseSpec {

  val mockTaiConnector: TaiConnector = mock[TaiConnector]
  val mockTaxCalcConnector: TaxCalcConnector = mock[TaxCalcConnector]

  val service = new PreviousYearSummaryService(mockTaiConnector, 5)

  def mockEmployments(f: Future[Seq[Employment]] = Future successful employmentData) =
    (mockTaiConnector
      .getEmployments(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
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

  def mockGetBenefits(f: Future[Benefits]) =
    (mockTaiConnector
      .getBenefits(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(f)

  def mockGetTaxCodes(f: Future[Seq[TaxCodeRecord]] = Future successful taxCodeData) =
    (mockTaiConnector
      .getTaxCodesForYear(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(f)

  "getMobilePayePreviousYearSummaryResponse" should {
    "return full MobilePayePreviousYearSummaryResponse when all data is available" in {
      mockEmployments()
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodes()

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      result shouldBe fullMobilePayePreviousYearResponse()

    }

    "return MobilePayePreviousYearSummaryResponse with no employments when employment data is missing" in {
      mockEmployments(Future successful Seq(taiEmployment3.copy(name = "ALDI", receivingOccupationalPension = true)))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodes()

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      result shouldBe fullMobilePayePreviousYearResponse().copy(employments = None, previousEmployments = None)
    }

    "return MobilePayePreviousYearSummaryResponse with no pensions when pension data is missing" in {
      mockEmployments(
        Future successful Seq(taiEmployment(TaxYear.current.previous.startYear), taiEmployment2.copy(employmentStatus = Ceased))
      )
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodes()

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      result shouldBe fullMobilePayePreviousYearResponse()
        .copy(pensions = None)
    }

    "return MobilePayePreviousYearSummaryResponse with no otherIncomes when OtherIncome data is missing" in {
      mockEmployments()
      mockNonTaxCodeIncomes(
        Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest.copy(otherNonTaxCodeIncomes = Nil))
      )
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodes()

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      result shouldBe fullMobilePayePreviousYearResponse()
        .copy(otherIncomes = None)
    }

    "return MobilePayePreviousYearSummaryResponse with correct employment payments" in {
      mockEmployments()
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodes()

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))
      result.employments.get.head.payments.get.size shouldBe 3
    }

    "return MobilePayePreviousYearSummaryResponse with no payments node for employment with no payments" in {
      mockEmployments(
        Future successful Seq(taiEmployment(TaxYear.current.previous.startYear).copy(annualAccounts = Seq.empty))
      )
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodes()

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      val payments = result.employments.get.head.payments

      payments shouldBe None
    }

    "return Not Found when tax year provided is beyond the history limit" in {

      intercept[NotFoundException] {
        await(service.getMobilePayePreviousYearSummaryResponse(nino, currentTaxYear - 6))
      }
    }

    "throw NotFoundException when receiving Error from taiConnector" in {
      mockEmployments(Future.failed(new InternalServerException("Internal Server Error")))

      intercept[NotFoundException] {
        await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))
      }
    }

  }

}
