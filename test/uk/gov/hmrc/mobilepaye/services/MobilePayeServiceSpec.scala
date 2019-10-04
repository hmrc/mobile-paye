/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.mobilepaye.domain.{IncomeSource, MobilePayeResponse}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class MobilePayeServiceSpec extends BaseSpec {

  val mockTaiConnector:     TaiConnector     = mock[TaiConnector]
  val mockTaxCalcConnector: TaxCalcConnector = mock[TaxCalcConnector]

  val service = new MobilePayeService(mockTaiConnector, mockTaxCalcConnector)

  def mockMatchingTaxCode(f: Future[Seq[IncomeSource]]) =
    (mockTaiConnector.getMatchingTaxCodeIncomes(_: Nino, _: Int, _: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*,*,*,*,*,*)
      .returning(f)

  def mockNonTaxCodeIncomes(f: Future[NonTaxCodeIncome]) =
    (mockTaiConnector.getNonTaxCodeIncome(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *, *).returning(f)

  def mockTaxAccountSummary(f: Future[TaxAccountSummary]) =
    (mockTaiConnector.getTaxAccountSummary(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *, *).returning(f)

  def mockP800Summary() =
    (mockTaxCalcConnector.getTaxReconciliations(_: Nino)(_: HeaderCarrier)).expects(*, *).returning(Future.successful(None))

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
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest.copy(otherNonTaxCodeIncomes = Nil)))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
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
      mockMatchingTaxCode(Future.failed(new Exception("cannot complete a coding calculation without a primary employment")))

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