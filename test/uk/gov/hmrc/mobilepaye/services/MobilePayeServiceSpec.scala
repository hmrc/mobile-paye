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
import uk.gov.hmrc.mobilepaye.connectors.{TaiConnector, TaxCalcConnector, TaxCalcConnectorSpec}
import uk.gov.hmrc.mobilepaye.domain.MobilePayeResponse
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class MobilePayeServiceSpec extends BaseSpec {

  val mockTaiConnector:     TaiConnector     = mock[TaiConnector]
  val mockTaxCalcConnector: TaxCalcConnector = mock[TaxCalcConnector]

  val service = new MobilePayeService(mockTaiConnector, mockTaxCalcConnector)

  def mockTaxCodeIncomes(f: Future[Seq[TaxCodeIncome]]) =
    (mockTaiConnector.getTaxCodeIncomes(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *, *).returning(f)

  def mockNonTaxCodeIncomes(f: Future[NonTaxCodeIncome]) =
    (mockTaiConnector.getNonTaxCodeIncome(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *, *).returning(f)

  def mockEmployments(f: Future[Seq[Employment]]) =
    (mockTaiConnector.getEmployments(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *, *).returning(f)

  def mockTaxAccountSummary(f: Future[TaxAccountSummary]) =
    (mockTaiConnector.getTaxAccountSummary(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *, *).returning(f)

  def mockP800Summary() =
    (mockTaxCalcConnector.getP800Summary(_: Nino, _: Int)(_: HeaderCarrier)).expects(*, *, *).returning(Future.successful(None))

  "getMobilePayeResponse" should {
    "return full MobilePayeResponse when all data is available" in {
      mockTaxCodeIncomes(Future.successful(taxCodeIncomes))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse
    }

    "return MobilePayeResponse with no employments when employment data is missing" in {
      mockTaxCodeIncomes(Future.successful(taxCodeIncomes.filter(_.componentType == PensionIncome)))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(employments = None)
    }

    "return MobilePayeResponse with no pensions when pension data is missing" in {
      mockTaxCodeIncomes(Future.successful(taxCodeIncomes.filter(_.componentType == EmploymentIncome)))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return MobilePayeResponse with no otherIncomes when OtherIncome data is missing" in {
      mockTaxCodeIncomes(Future.successful(taxCodeIncomes))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
    }

    "throw UnauthorizedException when receiving UnauthorizedException from taiConnector" in {
      mockTaxCodeIncomes(Future.failed(new UnauthorizedException("Unauthorized")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      intercept[UnauthorizedException] {
        await(service.getMobilePayeResponse(nino, currentTaxYear))
      }
    }

    "throw ForbiddenException when receiving ForbiddenException from taiConnector" in {
      mockTaxCodeIncomes(Future.failed(new ForbiddenException("Forbidden")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      intercept[ForbiddenException] {
        await(service.getMobilePayeResponse(nino, currentTaxYear))
      }
    }

    "throw InternalServerError when receiving InternalServerError from taiConnector" in {
      mockTaxCodeIncomes(Future.failed(new InternalServerException("Internal Server Error")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      intercept[InternalServerException] {
        await(service.getMobilePayeResponse(nino, currentTaxYear))
      }
    }

    "return an empty MobilePayeResponse when an exception is thrown that contains 'no employments recorded for current tax year'" in {
      mockTaxCodeIncomes(Future.failed(new Exception("no employments recorded for current tax year")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe MobilePayeResponse.empty
    }

    "return an empty MobilePayeResponse when an exception is thrown from NPS that contains 'cannot complete a coding calculation without a primary employment'" in {
      mockTaxCodeIncomes(Future.failed(new Exception("cannot complete a coding calculation without a primary employment")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe MobilePayeResponse.empty
    }

    "return an empty MobilePayeResponse when an exception is thrown from NPS that contains 'no employments recorded for this individual'" in {
      mockTaxCodeIncomes(Future.failed(new Exception("no employments recorded for this individual")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()

      val result = await(service.getMobilePayeResponse(nino, currentTaxYear))

      result shouldBe MobilePayeResponse.empty
    }
  }

}