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
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, HttpResponse, InternalServerException, NotFoundException, UnauthorizedException}
import uk.gov.hmrc.mobilepaye.connectors.{FeedbackConnector, MobileSimpleAssessmentConnector, TaiConnector, TaxCalcConnector}
import uk.gov.hmrc.mobilepaye.domain.simpleassessment.MobileSimpleAssessmentResponse
import uk.gov.hmrc.mobilepaye.domain.{Feedback, IncomeSource, MobilePayeSummaryResponse, OtherBenefits, P800Cache, PayeIncome, TaxCodeChange}
import uk.gov.hmrc.mobilepaye.domain.tai._
import uk.gov.hmrc.mobilepaye.domain.taxcalc.TaxYearReconciliation
import uk.gov.hmrc.mobilepaye.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class MobilePayeServiceSpec extends BaseSpec with DefaultPlayMongoRepositorySupport[P800Cache] {

  override lazy val repository = new P800CacheMongo(mongoComponent, appConfig)

  val mockTaiConnector:                    TaiConnector                    = mock[TaiConnector]
  val mockTaxCalcConnector:                TaxCalcConnector                = mock[TaxCalcConnector]
  val mockFeedbackConnector:               FeedbackConnector               = mock[FeedbackConnector]
  val mockMobileSimpleAssessmentConnector: MobileSimpleAssessmentConnector = mock[MobileSimpleAssessmentConnector]
  val p800CacheMongo:                      P800CacheMongo                  = repository
  val dateFormatter:                       DateTimeFormatter               = DateTimeFormatter.ofPattern("yyyy-MM-dd\'T\'HH:mm:ss")
  val inactiveDate:                        String                          = "2020-02-01T00:00:00"
  val activeStartDate:                     String                          = LocalDateTime.now(ZoneId.of("Europe/London")).minusDays(10).format(dateFormatter)
  val activeEndDate:                       String                          = LocalDateTime.now(ZoneId.of("Europe/London")).plusDays(10).format(dateFormatter)

  val service = new MobilePayeService(mockTaiConnector,
                                      mockTaxCalcConnector,
                                      p800CacheMongo,
                                      mockFeedbackConnector,
                                      mockMobileSimpleAssessmentConnector,
                                      inactiveDate,
                                      inactiveDate,
                                      inactiveDate,
                                      inactiveDate,
                                      inactiveDate,
                                      inactiveDate,
                                      true,
                                      true,
                                      true,
                                      5)

  def mockMatchingTaxCodeLive(f: Future[Seq[IncomeSource]]) =
    (mockTaiConnector
      .getMatchingTaxCodeIncomes(_: Nino, _: Int, _: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, "Live", *, *)
      .returning(f)

  def mockMatchingTaxCodeNotLive(f: Future[Seq[IncomeSource]]) =
    (mockTaiConnector
      .getMatchingTaxCodeIncomes(_: Nino, _: Int, _: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, "NotLive", *, *)
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

  def mockPostFeedback(f: Future[HttpResponse]) =
    (mockFeedbackConnector
      .postFeedback(_: Feedback)(_: HeaderCarrier))
      .expects(*, *)
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

  "getMobilePayeSummaryResponse" should {
    "return full MobilePayeResponse when all data is available" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful employmentIncomeSource ++ employmentIncomeSource)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary(Some(taxCalcTaxYearReconciliationResponse))
      mockGetSimpleAssessmentLiabilities(Future successful Some(fullMobileSimpleAssessmentResponse))

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(previousEmployments = Some((employments ++ employments)),
                                                  simpleAssessment = Some(fullMobileSimpleAssessmentResponse))
    }

    "return full MobilePayeResponse with tax comparison link during Welsh active period" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSourceWelsh))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockCYPlusOneAccountSummary(Future successful true)
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          mockFeedbackConnector,
                                          mockMobileSimpleAssessmentConnector,
                                          inactiveDate,
                                          inactiveDate,
                                          activeStartDate,
                                          activeEndDate,
                                          inactiveDate,
                                          inactiveDate,
                                          true,
                                          true,
                                          true,
                                          5)

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponseWithCY1Link.copy(taxCodeLocation = Some("Welsh"),
                                                             employments = Some(welshEmployments))
    }

    "return full MobilePayeResponse with tax comparison link during UK active period" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSourceUK))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockCYPlusOneAccountSummary(Future successful true)
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          mockFeedbackConnector,
                                          mockMobileSimpleAssessmentConnector,
                                          activeStartDate,
                                          activeEndDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          true,
                                          true,
                                          true,
                                          5)

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponseWithCY1Link.copy(taxCodeLocation = Some("rUK"),
                                                             employments = Some(ukEmployments))
    }

    "return full MobilePayeResponse with tax comparison link during Scottish active period" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockCYPlusOneAccountSummary(Future successful true)
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          mockFeedbackConnector,
                                          mockMobileSimpleAssessmentConnector,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          activeStartDate,
                                          activeEndDate,
                                          true,
                                          true,
                                          true,
                                          5)

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponseWithCY1Link
    }

    "return full MobilePayeResponse with no tax comparison link if not in active period" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSourceWelsh))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          mockFeedbackConnector,
                                          mockMobileSimpleAssessmentConnector,
                                          activeStartDate,
                                          activeEndDate,
                                          inactiveDate,
                                          inactiveDate,
                                          activeStartDate,
                                          activeEndDate,
                                          true,
                                          true,
                                          true,
                                          5)

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(employments = Some(welshEmployments))
    }

    "return MobilePayeResponse with no untaxed interest" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = Some(Seq(otherIncome)))
    }

    "return MobilePayeResponse with no employments when employment data is missing" in {
      mockMatchingTaxCodeLive(Future.successful(Seq.empty[IncomeSource]))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(employments = None)
    }

    "return MobilePayeResponse with no pensions when pension data is missing" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(Seq.empty[IncomeSource]))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return MobilePayeResponse with no otherIncomes when OtherIncome data is missing" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(
        Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest.copy(otherNonTaxCodeIncomes = Nil))
      )
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
    }

    "return MobilePayeResponse with correct employment latestPayments" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val result         = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
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
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource2))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val result         = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
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
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSourceNoPayments))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSourceNoPension))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      val payments = result.employments.get.head.payments

      payments shouldBe None
    }

    "return MobilePayeResponse with no latestPayment for pension" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
      mockP800Summary()

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result.pensions.get.head.latestPayment shouldBe None
    }

    "return full MobilePayeResponse with employment benefits data totalled correctly" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((allBenefits)))
      mockGetTaxCodeChangeExists(Future.successful(true))
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
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))
      mockP800Summary()

      val service = new MobilePayeService(mockTaiConnector,
                                          mockTaxCalcConnector,
                                          p800CacheMongo,
                                          mockFeedbackConnector,
                                          mockMobileSimpleAssessmentConnector,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          inactiveDate,
                                          true,
                                          false,
                                          true,
                                          5)

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe fullMobilePayeResponse.copy(taxCodeChange = Some(TaxCodeChange(false)))
    }

    "throw UnauthorizedException when receiving UnauthorizedException from taiConnector" in {
      mockMatchingTaxCodeLive(Future.failed(new UnauthorizedException("Unauthorized")))

      intercept[UnauthorizedException] {
        await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
      }
    }

    "throw ForbiddenException when receiving ForbiddenException from taiConnector" in {
      mockMatchingTaxCodeLive(Future.failed(new ForbiddenException("Forbidden")))

      intercept[ForbiddenException] {
        await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
      }
    }

    "throw InternalServerError when receiving InternalServerError from taiConnector" in {
      mockMatchingTaxCodeLive(Future.failed(new InternalServerException("Internal Server Error")))

      intercept[InternalServerException] {
        await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))
      }
    }

    "return an empty MobilePayeResponse when an exception is thrown that contains 'no employments recorded for current tax year'" in {
      mockMatchingTaxCodeLive(Future.failed(new Exception("no employments recorded for current tax year")))

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe MobilePayeSummaryResponse.empty
    }

    "return an empty MobilePayeResponse when an exception is thrown from NPS that contains 'cannot complete a coding calculation without a primary employment'" in {
      mockMatchingTaxCodeLive(
        Future.failed(new Exception("cannot complete a coding calculation without a primary employment"))
      )

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe MobilePayeSummaryResponse.empty
    }

    "return an empty MobilePayeResponse when an exception is thrown from NPS that contains 'no employments recorded for this individual'" in {
      mockMatchingTaxCodeLive(Future.failed(new Exception("no employments recorded for this individual")))

      val result = await(service.getMobilePayeSummaryResponse(nino, currentTaxYear, journeyId))

      result shouldBe MobilePayeSummaryResponse.empty
    }

  }

  "getMobilePayePreviousYearSummaryResponse" should {
    "return full MobilePayeResponse when all data is available" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful employmentIncomeSource ++ employmentIncomeSource)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      result shouldBe fullMobilePayePreviousYearResponse().copy(previousEmployments = Some((employments ++ employments))
      )
    }

    "return MobilePayeResponse with no employments when employment data is missing" in {
      mockMatchingTaxCodeLive(Future.successful(Seq.empty[IncomeSource]))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      result shouldBe fullMobilePayePreviousYearResponse().copy(employments = None, previousEmployments = None)
    }

    "return MobilePayeResponse with no pensions when pension data is missing" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(Seq.empty[IncomeSource]))
      mockMatchingTaxCodeNotLive(Future successful employmentIncomeSource ++ employmentIncomeSource)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      result shouldBe fullMobilePayePreviousYearResponse()
        .copy(pensions = None, previousEmployments = Some((employments ++ employments)))
    }

    "return MobilePayeResponse with no otherIncomes when OtherIncome data is missing" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful employmentIncomeSource ++ employmentIncomeSource)
      mockNonTaxCodeIncomes(
        Future.successful(nonTaxCodeIncomeWithoutUntaxedInterest.copy(otherNonTaxCodeIncomes = Nil))
      )
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))

      result shouldBe fullMobilePayePreviousYearResponse()
        .copy(otherIncomes = None, previousEmployments = Some((employments ++ employments)))
    }

    "return MobilePayeResponse with correct employment payments" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSource2))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSource))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))

      val result = await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))
      result.employments.get.head.payments.get.size shouldBe 3
    }

    "return MobilePayeResponse with no payments node for employment with no payments" in {
      mockMatchingTaxCodeLive(Future.successful(employmentIncomeSourceNoPayments))
      mockMatchingTaxCodeLive(Future.successful(pensionIncomeSourceNoPension))
      mockMatchingTaxCodeNotLive(Future successful Seq.empty)
      mockNonTaxCodeIncomes(Future.successful(nonTaxCodeIncomeWithUntaxedInterest))
      mockTaxAccountSummary(Future.successful(taxAccountSummary))
      mockGetBenefits(Future.successful((noBenefits)))

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
      mockMatchingTaxCodeLive(Future.failed(new InternalServerException("Internal Server Error")))

      intercept[NotFoundException] {
        await(service.getMobilePayePreviousYearSummaryResponse(nino, previousTaxYear))
      }
    }

  }

  "postFeedback" should {

    "return a 201 No Content" when {

      "a valid feedbackModel has been provided" in {
        mockPostFeedback(Future.successful(HttpResponse.apply(204, "")))

        val result = await(service.postFeedback(feedbackModel))

        result.status shouldBe 204

      }

    }
  }

}
