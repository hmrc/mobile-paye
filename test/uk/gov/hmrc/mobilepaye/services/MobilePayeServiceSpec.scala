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

package uk.gov.hmrc.mobilepaye.services

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, InternalServerException, UnauthorizedException}
import uk.gov.hmrc.mobilepaye.connectors.TaiConnector
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}


class MobilePayeServiceSpec extends BaseSpec {

  val mockTaiConnector: TaiConnector = mock[TaiConnector]

  val service = new MobilePayeService(mockTaiConnector)

  def mockTaxCodeIncomes(f: Future[Seq[TaxCodeIncome]]): Unit = {
    (mockTaiConnector.getTaxCodeIncomes(_: Nino)
    (_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *).returning(f)
  }

  def mockNonTaxCodeIncomes(f: Future[NonTaxCodeIncome]): Unit = {
    (mockTaiConnector.getNonTaxCodeIncome(_: Nino)
    (_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *).returning(f)
  }

  def mockEmployments(f: Future[Seq[Employment]]): Unit = {
    (mockTaiConnector.getEmployments(_: Nino)
    (_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *).returning(f)
  }

  def mockTaxAccountSummary(f: Future[TaxAccountSummary]): Unit = {
    (mockTaiConnector.getTaxAccountSummary(_: Nino)
    (_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *).returning(f)
  }

  "getMobilePayeResponse" should {
    "return full MobilePayeResponse when all data is available" in {
      mockTaxCodeIncomes(Future.successful(taxCodeIncomes))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))

      val result = await(service.getMobilePayeResponse(nino))

      result shouldBe fullMobilePayeResponse
    }

    "return MobilePayeResponse with no employments when employment data is missing" in {
      mockTaxCodeIncomes(Future.successful(taxCodeIncomes.filter(_.componentType == PensionIncome)))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))

      val result = await(service.getMobilePayeResponse(nino))

      result shouldBe fullMobilePayeResponse.copy(employments = None)
    }

    "return MobilePayeResponse with no pensions when pension data is missing" in {
      mockTaxCodeIncomes(Future.successful(taxCodeIncomes.filter(_.componentType == EmploymentIncome)))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))

      val result = await(service.getMobilePayeResponse(nino))

      result shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return MobilePayeResponse with no otherIncomes when OtherIncome data is missing" in {
      mockTaxCodeIncomes(Future.successful(taxCodeIncomes))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))

      val result = await(service.getMobilePayeResponse(nino))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
    }

    "throw UnauthorizedException when receiving UnauthorizedException from taiConnector" in {
      mockTaxCodeIncomes(Future.failed(new UnauthorizedException("Unauthorized")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))

      intercept[UnauthorizedException] {
        await(service.getMobilePayeResponse(nino))
      }
    }

    "throw ForbiddenException when receiving ForbiddenException from taiConnector" in {
      mockTaxCodeIncomes(Future.failed(new ForbiddenException("Forbidden")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))

      intercept[ForbiddenException] {
        await(service.getMobilePayeResponse(nino))
      }
    }

    "throw InternalServerError when receiving InternalServerError from taiConnector" in {
      mockTaxCodeIncomes(Future.failed(new InternalServerException("Internal Server Error")))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil)))
      mockEmployments(Future.successful(taiEmployments))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))

      intercept[InternalServerException] {
        await(service.getMobilePayeResponse(nino))
      }
    }
  }
}