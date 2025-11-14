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
import uk.gov.hmrc.mobilepaye.connectors.*
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.MobileSimpleAssessmentResponse
import uk.gov.hmrc.mobilepaye.domain.tai.*
import uk.gov.hmrc.mobilepaye.domain.taxcalc.TaxYearReconciliation
import uk.gov.hmrc.mobilepaye.domain.types.JourneyId
import uk.gov.hmrc.mobilepaye.domain.{CarBenefit, MedicalInsurance, *}
import uk.gov.hmrc.mobilepaye.mocks.ShutteringMock
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
import uk.gov.hmrc.mobilepaye.utils.{BaseSpec, PlayMongoRepositorySupport}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class MobilePayeServiceSpec extends BaseSpec with PlayMongoRepositorySupport[P800Cache] with ShutteringMock {

  override val repository: P800CacheMongo = new P800CacheMongo(mongoComponent, appConfig)

  implicit val mockShutteringConnector: ShutteringConnector = mock[ShutteringConnector]
  val mockTaiConnector: TaiConnector = mock[TaiConnector]
  val mockTaxCalcConnector: TaxCalcConnector = mock[TaxCalcConnector]
  val mockMobileSimpleAssessmentConnector: MobileSimpleAssessmentConnector = mock[MobileSimpleAssessmentConnector]
  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val p800CacheMongo: P800CacheMongo = repository
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd\'T\'HH:mm:ss")
  val inactiveDate: String = "2020-02-01T00:00:00"
  val activeStartDate: String = LocalDateTime.now(ZoneId.of("Europe/London")).minusDays(10).format(dateFormatter)
  val activeEndDate: String = LocalDateTime.now(ZoneId.of("Europe/London")).plusDays(10).format(dateFormatter)

  val service = new MobilePayeService(
    mockTaiConnector,
    mockTaxCalcConnector,
    p800CacheMongo,
    mockMobileSimpleAssessmentConnector,
    mockCitizenDetailsConnector,
    mockShutteringConnector,
    inactiveDate,
    inactiveDate,
    inactiveDate,
    inactiveDate,
    inactiveDate,
    inactiveDate,
    true,
    true,
    true
  )

  def mockMatchingTaxCodeAll(f: Future[Seq[IncomeSource]]) =
    (mockTaiConnector
      .getMatchingTaxCodeIncomes(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
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

  def mockP800Summary(f: Option[List[TaxYearReconciliation]] = None) =
    (mockTaxCalcConnector
      .getTaxReconciliations(_: Nino)(_: HeaderCarrier))
      .expects(*, *)
      .returning(Future.successful(f))

  def mockCYPlusOneAccountSummary(f: Future[Boolean]) =
    (mockTaiConnector
      .getCYPlusOneAccountSummary(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(f)

  def mockGetBenefits(f: Future[Benefits]) =
    (mockTaiConnector
      .getBenefits(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(f)

  def mockGetTaxCodeChangeExists(f: Future[Boolean]) =
    (mockTaiConnector
      .getTaxCodeChangeExists(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returning(f)

  def mockGetSimpleAssessmentLiabilities(f: Future[Option[MobileSimpleAssessmentResponse]]) =
    (mockMobileSimpleAssessmentConnector
      .getSimpleAssessmentLiabilities(_: JourneyId)(_: HeaderCarrier))
      .expects(*, *)
      .returning(f)

  def mockGetTaxCodeChange(f: Future[TaxCodeChangeDetails]) =
    (mockTaiConnector
      .getTaxCodeChange(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returning(f)

  "getMobilePayeSummaryResponse" should {
    "return full MobilePayeResponse when all data is available" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource ++ pensionIncomeSourceNew ++ Seq(incomeSourceCeased)))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockGetSimpleAssessmentLiabilities(Future successful Some(fullMobileSimpleAssessmentResponse))
      mockShutteringResponse(Shuttering.shutteringDisabled)
      mockP800Summary(Some(taxCalcTaxYearReconciliationResponse))

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(
        previousEmployments = Some(Seq(PayeIncome.fromIncomeSource(incomeSourceCeased, true))),
        simpleAssessment    = Some(fullMobileSimpleAssessmentResponse),
        pensions            = Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
      )
    }

    "return full MobilePayeResponse with isRTIDown as true when all data is available and RTi is down for oneEmployemnt " in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSourceWithRtiUnavail ++ pensionIncomeSourceNew ++ Seq(incomeSourceCeased)))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockGetSimpleAssessmentLiabilities(Future successful Some(fullMobileSimpleAssessmentResponse))
      mockShutteringResponse(Shuttering.shutteringDisabled)
      mockP800Summary(Some(taxCalcTaxYearReconciliationResponse))

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(
        previousEmployments = Some(Seq(PayeIncome.fromIncomeSource(incomeSourceCeased, true))),
        simpleAssessment    = Some(fullMobileSimpleAssessmentResponse),
        isRTIDown           = true,
        pensions            = Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
      )
    }

    "return full MobilePayeResponse with tax comparison link during Welsh active period" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSourceWelsh ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockP800Summary()
      mockCYPlusOneAccountSummary(Future successful true)
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)

      val service = new MobilePayeService(
        mockTaiConnector,
        mockTaxCalcConnector,
        p800CacheMongo,
        mockMobileSimpleAssessmentConnector,
        mockCitizenDetailsConnector,
        mockShutteringConnector,
        inactiveDate,
        inactiveDate,
        activeStartDate,
        activeEndDate,
        inactiveDate,
        inactiveDate,
        true,
        true,
        true
      )

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponseWithCY1Link.copy(
        taxCodeLocation = Some("Welsh"),
        employments     = Some(welshEmployments),
        pensions        = Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
      )
    }

    "return full MobilePayeResponse with tax comparison link during UK active period" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSourceUK ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockCYPlusOneAccountSummary(Future successful true)
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary(None)

      val service = new MobilePayeService(
        mockTaiConnector,
        mockTaxCalcConnector,
        p800CacheMongo,
        mockMobileSimpleAssessmentConnector,
        mockCitizenDetailsConnector,
        mockShutteringConnector,
        activeStartDate,
        activeEndDate,
        inactiveDate,
        inactiveDate,
        inactiveDate,
        inactiveDate,
        true,
        true,
        true
      )

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponseWithCY1Link.copy(
        taxCodeLocation = Some("rUK"),
        employments     = Some(ukEmployments),
        pensions        = Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
      )
    }

    "return full MobilePayeResponse with tax comparison link during Scottish active period" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockCYPlusOneAccountSummary(Future successful true)
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val service = new MobilePayeService(
        mockTaiConnector,
        mockTaxCalcConnector,
        p800CacheMongo,
        mockMobileSimpleAssessmentConnector,
        mockCitizenDetailsConnector,
        mockShutteringConnector,
        inactiveDate,
        inactiveDate,
        inactiveDate,
        inactiveDate,
        activeStartDate,
        activeEndDate,
        true,
        true,
        true
      )

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponseWithCY1Link.copy(pensions =
        Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
      )
    }

    "return full MobilePayeResponse with no tax comparison link if not in active period" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSourceWelsh ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val service = new MobilePayeService(
        mockTaiConnector,
        mockTaxCalcConnector,
        p800CacheMongo,
        mockMobileSimpleAssessmentConnector,
        mockCitizenDetailsConnector,
        mockShutteringConnector,
        activeStartDate,
        activeEndDate,
        inactiveDate,
        inactiveDate,
        activeStartDate,
        activeEndDate,
        true,
        true,
        true
      )

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(employments = Some(welshEmployments),
                                                  pensions =
                                                    Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
                                                 )
    }

    "return MobilePayeResponse with no untaxed interest" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = Some(Seq(otherIncome)),
                                                  pensions =
                                                    Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
                                                 )
    }

    "return MobilePayeResponse with no employments when employment data is missing" in {
      mockMatchingTaxCodeAll(Future.successful(Seq.empty[IncomeSource] ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(employments = None,
                                                  pensions =
                                                    Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
                                                 )
    }

    "return MobilePayeResponse with no pensions when pension data is missing" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return MobilePayeResponse with no otherIncomes when OtherIncome data is missing" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(
        Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest.copy(otherNonTaxCodeIncomes = Nil))
      )
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = None,
                                                  pensions =
                                                    Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
                                                 )
    }

    "return MobilePayeResponse with correct Payments" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource2 ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
      result.employments.get.head.payments.get.size shouldBe 3
    }

    "return MobilePayeResponse with no payments node for employment with no payments" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSourceNoPayments))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      val payments = result.employments.get.head.payments

      payments shouldBe None
    }

    "return full MobilePayeResponse with employment benefits data totalled correctly" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(allBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      val employment1 = result.employments.get.head
      val employment2 = result.employments.get.last
      employment1.employmentBenefits.get.benefits.size shouldBe 3
      employment1.employmentBenefits.get.benefits
        .filter(_.benefitType.toString == CarBenefit.toString)
        .head
        .amount shouldBe BigDecimal(20000)
      employment1.employmentBenefits.get.benefits
        .filter(_.benefitType.toString == MedicalInsurance.toString)
        .head
        .amount shouldBe BigDecimal(650)
      employment1.employmentBenefits.get.benefits
        .filter(_.benefitType.toString == OtherBenefits.toString)
        .head
        .amount                                        shouldBe BigDecimal(450)
      employment2.employmentBenefits.get.benefits.size shouldBe 2
      employment2.employmentBenefits.get.benefits
        .filter(_.benefitType.toString == MedicalInsurance.toString)
        .head
        .amount shouldBe BigDecimal(350)
      employment2.employmentBenefits.get.benefits
        .filter(_.benefitType.toString == OtherBenefits.toString)
        .head
        .amount shouldBe BigDecimal(100)

    }

    "return tax code change as false if flag disabled" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource ++ pensionIncomeSourceNew))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockP800Summary()

      val service = new MobilePayeService(
        mockTaiConnector,
        mockTaxCalcConnector,
        p800CacheMongo,
        mockMobileSimpleAssessmentConnector,
        mockCitizenDetailsConnector,
        mockShutteringConnector,
        inactiveDate,
        inactiveDate,
        inactiveDate,
        inactiveDate,
        inactiveDate,
        inactiveDate,
        true,
        false,
        true
      )

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(taxCodeChange = Some(TaxCodeChange(hasChanged = false)),
                                                  pensions =
                                                    Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
                                                 )
    }

    "throw UnauthorizedException when receiving UnauthorizedException from taiConnector" in {
      mockMatchingTaxCodeAll(Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
      }
    }

    "throw ForbiddenException when receiving ForbiddenException from taiConnector" in {
      mockMatchingTaxCodeAll(Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException] {
        await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
      }
    }

    "throw InternalServerError when receiving InternalServerError from taiConnector" in {
      mockMatchingTaxCodeAll(Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
      }
    }

    "return an empty MobilePayeResponse when an exception is thrown that contains 'no employments recorded for current tax year'" in {
      mockMatchingTaxCodeAll(Future.failed(new Exception("no employments recorded for current tax year")))

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe MobilePayeSummaryResponse.empty
    }

    "return an empty MobilePayeResponse when an exception is thrown from NPS that contains 'cannot complete a coding calculation without a primary employment'" in {
      mockMatchingTaxCodeAll(
        Future.failed(new Exception("cannot complete a coding calculation without a primary employment"))
      )

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe MobilePayeSummaryResponse.empty
    }

    "return an empty MobilePayeResponse when an exception is thrown from NPS that contains 'no employments recorded for this individual'" in {
      mockMatchingTaxCodeAll(Future.failed(new Exception("no employments recorded for this individual")))

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe MobilePayeSummaryResponse.empty
    }

    "return full MobilePayeResponse without repayment data when p800 is shuttered" in {
      mockMatchingTaxCodeAll(Future.successful(employmentIncomeSource ++ pensionIncomeSourceNew ++ Seq(incomeSourceCeased)))
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful(noBenefits))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)
      mockP800Summary(Some(taxCalcTaxYearReconciliationResponse))
      mockGetSimpleAssessmentLiabilities(Future successful Some(fullMobileSimpleAssessmentResponse))
      mockShutteringResponse(Shuttering(shuttered = true))

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(
        simpleAssessment    = Some(fullMobileSimpleAssessmentResponse),
        repayment           = None,
        previousEmployments = Some(Seq(PayeIncome.fromIncomeSource(incomeSourceCeased, true))),
        pensions            = Some(pensionIncomeSourceNew.map(ic => PayeIncome.fromIncomeSource(ic, employment = false)))
      )
    }

  }

  "getCurrentTaxCode" should {
    "return a 200 with the taxCode if a single, current tax code returned from tai" in {
      mockGetTaxCodeChange(Future successful taxCodeChangeDetails)

      val result = await(service.getCurrentTaxCode(nino))

      result shouldBe Some(taxCodeRecord.taxCode)
    }

    "return a None if no tax codes returned from tai" in {
      mockGetTaxCodeChange(Future successful TaxCodeChangeDetails(Seq.empty, Seq.empty))

      val result = await(service.getCurrentTaxCode(nino))

      result shouldBe None
    }

    "return a None if no current tax codes returned from tai" in {
      mockGetTaxCodeChange(Future successful TaxCodeChangeDetails(Seq.empty, Seq(taxCodeRecord, taxCodeRecord2)))

      val result = await(service.getCurrentTaxCode(nino))

      result shouldBe None
    }

    "return a None if more than 1 current tax codes returned from tai" in {
      mockGetTaxCodeChange(Future successful TaxCodeChangeDetails(Seq(taxCodeRecord, taxCodeRecord2), Seq.empty))

      val result = await(service.getCurrentTaxCode(nino))

      result shouldBe None
    }
  }

}
