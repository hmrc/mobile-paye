//import org.mockito.ArgumentMatchers.any
//import org.mockito.Mockito.when
//import play.api.Application
//import play.api.inject.guice.GuiceApplicationBuilder
//import play.api.libs.json.{JsValue, Json}
//import play.api.libs.ws.WSResponse
//import play.api.test.Injecting
//import stubs.AuthStub.*
//import stubs.MobileSimpleAssessmentStub.stubSimpleAssessmentResponse
//import stubs.ShutteringStub.*
//import stubs.TaiStub.*
//import stubs.TaxCalcStub.*
//import stubs.CitizenDetailsStub.*
//import uk.gov.hmrc.domain.Nino
//import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, OnlinePaymentIntegration}
//import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig
//import uk.gov.hmrc.mobilepaye.domain.tai.{AnnualAccount, Available, CarBenefit, Ceased, MedicalInsurance, NotLive, Payment, PensionIncome, TaxCodeChangeDetails}
//import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status
//import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status.{Overpaid, Underpaid}
//import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus.*
//import uk.gov.hmrc.mobilepaye.domain.{IncomeTaxYear, MobilePayeSummaryResponse, OtherBenefits, P800Cache, P800Repayment, Shuttering}
//import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
//import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
//import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
//import uk.gov.hmrc.time.TaxYear
//import utils.BaseISpec
//import play.api.libs.ws.readableAsJson
//
//import scala.language.implicitConversions
//import java.time.LocalDate
//import scala.concurrent.Future
//import scala.util.Random
//
//class LiveMobilePayeControllerISpec extends BaseISpec with Injecting with PlayMongoRepositorySupport[P800Cache] {
//
//  val appConfig: MobilePayeConfig = MobilePayeConfig(app.configuration)
//
//  override val repository: PlayMongoRepository[P800Cache] = app.injector.instanceOf[P800CacheMongo]
//
//  lazy val urlWithCurrentYearAsInt =
//    s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//
//  lazy val urlWithCurrentYearAsCurrent =
//    s"/nino/$nino/tax-year/current/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//
//  lazy val urlWithPreviousYear =
//    s"/nino/$nino/previous-tax-year/$previousTaxYear/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//
//  lazy val incomeTaxHistoryUrl =
//    s"/nino/$nino/income-tax-history?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//
//  lazy val taxCodeUrl = s"/nino/$nino/tax-code?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//
//  override def beforeEach(): Unit = {
//    dropCollection()
//    when(mockFeatureFlagService.get(any()))
//      .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = false)))
//    super.beforeEach()
//  }
//  implicit def ninoToString(nino: Nino): String = nino.toString()
//
//  s"GET /nino/$nino/tax-year/$currentTaxYear/summary" should {
//
//    "return OK and a full valid MobilePayeResponse json" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(
//        nino,
//        employmentIncomeSource.map(incSrc => incSrc.copy(employment = incSrc.employment.copy(employmentStatus = NotLive))) ++ employmentIncomeSource
//          .map(incSrc => incSrc.copy(employment = incSrc.employment.copy(endDate = Some(LocalDate.of(2022, 2, 1)), employmentStatus = NotLive))),
//        status = NotLive
//      )
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryIsFound(nino, taxAccountSummary, cyPlusone = true)
//      taxCalcWithNoDate(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//      stubSimpleAssessmentResponse
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json
//        .toJson(
//          fullMobilePayeResponseWithCY1Link
//            .copy(
//              previousEmployments = Some(
//                employments.map(emp =>
//                  emp.copy(
//                    status                           = NotLive,
//                    incomeDetailsLink                = s"/check-income-tax/your-income-calculation-details/${emp.incomeDetailsLink.last}",
//                    yourIncomeCalculationDetailsLink = s"/check-income-tax/your-income-calculation-details/${emp.incomeDetailsLink.last}",
//                    updateIncomeLink                 = None
//                  )
//                ) ++
//                  employments.map(emp =>
//                    emp.copy(
//                      status                           = NotLive,
//                      incomeDetailsLink                = s"/check-income-tax/your-income-calculation-details/${emp.incomeDetailsLink.last}",
//                      yourIncomeCalculationDetailsLink = s"/check-income-tax/your-income-calculation-details/${emp.incomeDetailsLink.last}",
//                      updateIncomeLink                 = None,
//                      endDate                          = Some(LocalDate.of(2022, 2, 1))
//                    )
//                  )
//              ),
//              simpleAssessment = Some(fullMobileSimpleAssessmentResponse),
//              repayment = Some(
//                P800Repayment(
//                  amount          = Some(1000),
//                  paymentStatus   = Some(Refund),
//                  datePaid        = None,
//                  taxYear         = previousTaxYear,
//                  claimRefundLink = Some(s"/tax-you-paid/$previousTaxYear-$currentTaxYear/paid-too-much")
//                )
//              )
//            )
//        )
//
//    }
//
//    "return OK and a full valid MobilePayeResponse json with Rti down" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSourceWithRtiUnavail)
//      stubForEmploymentIncome(
//        nino,
//        employmentIncomeSource.map(incSrc => incSrc.copy(employment = incSrc.employment.copy(employmentStatus = NotLive))) ++ employmentIncomeSource
//          .map(incSrc => incSrc.copy(employment = incSrc.employment.copy(endDate = Some(LocalDate.of(2022, 2, 1)), employmentStatus = NotLive))),
//        status = NotLive
//      )
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryIsFound(nino, taxAccountSummary, cyPlusone = true)
//      taxCalcWithNoDate(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//      stubSimpleAssessmentResponse
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json
//        .toJson(
//          fullMobilePayeResponseWithCY1Link
//            .copy(
//              previousEmployments = Some(
//                employments.map(emp =>
//                  emp.copy(
//                    status                           = NotLive,
//                    incomeDetailsLink                = s"/check-income-tax/your-income-calculation-details/${emp.incomeDetailsLink.last}",
//                    yourIncomeCalculationDetailsLink = s"/check-income-tax/your-income-calculation-details/${emp.incomeDetailsLink.last}",
//                    updateIncomeLink                 = None
//                  )
//                ) ++
//                  employments.map(emp =>
//                    emp.copy(
//                      status                           = NotLive,
//                      incomeDetailsLink                = s"/check-income-tax/your-income-calculation-details/${emp.incomeDetailsLink.last}",
//                      yourIncomeCalculationDetailsLink = s"/check-income-tax/your-income-calculation-details/${emp.incomeDetailsLink.last}",
//                      updateIncomeLink                 = None,
//                      endDate                          = Some(LocalDate.of(2022, 2, 1))
//                    )
//                  )
//              ),
//              simpleAssessment = Some(fullMobileSimpleAssessmentResponse),
//              repayment = Some(
//                P800Repayment(
//                  amount          = Some(1000),
//                  paymentStatus   = Some(Refund),
//                  datePaid        = None,
//                  taxYear         = previousTaxYear,
//                  claimRefundLink = Some(s"/tax-you-paid/$previousTaxYear-$currentTaxYear/paid-too-much")
//                )
//              ),
//              isRTIDown = true
//            )
//        )
//
//    }
//
//    "return OK and a full valid MobilePayeResponse json with no comparison link if data not found" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//
//    }
//
//    "return OK and a valid MobilePayeResponse json without untaxed income but other income" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(
//        fullMobilePayeResponse
//          .copy(
//            otherIncomes = Some(Seq(otherIncome))
//          )
//      )
//    }
//
//    "return OK and a valid MobilePayeResponse json without employments" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, Seq.empty)
//      stubForEmploymentIncome(nino, status = NotLive)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse.copy(employments = None))
//    }
//
//    "return OK and a valid MobilePayeResponse json without pensions" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, Seq.empty)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse.copy(pensions = None))
//    }
//
//    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      stubForPensions(nino, pensionIncomeSource)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = None))
//    }
//
//    "return GONE when person is deceased" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person.copy(deceased = true))
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status shouldBe 410
//
//      employmentsNotCalled(nino)
//      pensionsNotCalled(nino)
//      nonTaxCodeIncomeNotCalled(nino)
//      taxAccountSummaryNotCalled(nino)
//      taxCalcCalled(nino, currentTaxYear)
//    }
//
//    "return LOCKED when person is locked in CID" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalLocked(nino)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status shouldBe 423
//
//      employmentsNotCalled(nino)
//      pensionsNotCalled(nino)
//      nonTaxCodeIncomeNotCalled(nino)
//      taxAccountSummaryNotCalled(nino)
//      taxCalcCalled(nino, currentTaxYear)
//    }
//
//    "return 429 when TAI returns too many requests" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalTooManyRequests(nino)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//
//      response.status shouldBe 429
//
//    }
//
//    "return 400 when no journeyId supplied" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//
//      val response =
//        await(wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary").addHttpHeaders(acceptJsonHeader).get())
//      response.status shouldBe 400
//    }
//
//    "return 400 when invalid journeyId supplied" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//
//      val response = await(
//        wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=ThisIsAnInvalidJourneyId")
//          .addHttpHeaders(acceptJsonHeader)
//          .get()
//      )
//      response.status shouldBe 400
//    }
//
//    "return 404 when json validation happen for get matching tax codes" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForJsonErrorEmploymentIncome(nino)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response.status shouldBe 404
//    }
//  }
//
//  s"GET /nino/$nino/tax-year/current/summary" should {
//
//    "return OK and a full valid MobilePayeResponse json" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//    }
//
//    "return OK and a full valid MobilePayeResponse json with Rti down" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//    }
//
//    "return OK and a valid MobilePayeResponse json without untaxed income but other income" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(
//        fullMobilePayeResponse
//          .copy(otherIncomes = Some(Seq(otherIncome)))
//      )
//    }
//
//    "return OK and a valid MobilePayeResponse json without employments" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, Seq.empty)
//      stubForEmploymentIncome(nino, status = NotLive)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse.copy(employments = None))
//    }
//
//    "return OK and a valid MobilePayeResponse json without pensions" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, Seq.empty)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse.copy(pensions = None))
//    }
//
//    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = None))
//    }
//
//    "return OK with P800Repayments for Overpaid tax and accepted RepaymentStatus" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      List(Refund, PaymentProcessing, PaymentPaid, ChequeSent)
//        .foreach { repaymentStatus =>
//          val amount = Random.nextDouble(): BigDecimal
//          val time = LocalDate.now
//
//          grantAccess(nino)
//          personalDetailsAreFound(nino, person)
//          stubForPensions(nino, pensionIncomeSource)
//          stubForEmploymentIncome(nino, employmentIncomeSource)
//          stubForEmploymentIncome(nino, status = NotLive)
//          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//          taxAccountSummaryIsFound(nino, taxAccountSummary)
//          taxAccountSummaryNotFound(nino, cyPlusone = true)
//          taxCalcValidResponse(nino, currentTaxYear - 1, amount, Overpaid, repaymentStatus, time)
//          stubForBenefits(nino, noBenefits)
//          stubForTaxCodeChangeExists(nino)
//          stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//          val expectedRepayment: Option[P800Repayment] =
//            repayment(P800Status.Overpaid, repaymentStatus, currentTaxYear, amount, time)
//
//          val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//          response.status shouldBe 200
//          response.body[JsValue] shouldBe Json.toJson(
//            fullMobilePayeResponse
//              .copy(
//                repayment = expectedRepayment
//              )
//          )
//        }
//    }
//
//    "return OK with no P800Repayments for Underpaid tax" in {
//      stubForShutteringDisabled()
//      List(PaymentDue, PartPaid, PaidAll, PaymentsDown, Unknown)
//        .foreach { repaymentStatus =>
//          val amount = Random.nextDouble(): BigDecimal
//          val time = LocalDate.now
//
//          grantAccess(nino)
//          personalDetailsAreFound(nino, person)
//          stubForPensions(nino, pensionIncomeSource)
//          stubForEmploymentIncome(nino, employmentIncomeSource)
//          stubForEmploymentIncome(nino, status = NotLive)
//          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//          taxAccountSummaryIsFound(nino, taxAccountSummary)
//          taxAccountSummaryNotFound(nino, cyPlusone = true)
//          taxCalcValidResponse(nino, currentTaxYear, amount, Underpaid, repaymentStatus, time)
//          stubForBenefits(nino, noBenefits)
//          stubForTaxCodeChangeExists(nino)
//          stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//          val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//          response.status        shouldBe 200
//          response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//        }
//    }
//
//    "return OK with no P800Repayments for uncovered RepaymentStatuses" in {
//      stubForShutteringDisabled()
//      List(SaUser, UnableToClaim)
//        .foreach { repaymentStatus =>
//          val amount = Random.nextDouble(): BigDecimal
//          val time = LocalDate.now
//
//          grantAccess(nino)
//          personalDetailsAreFound(nino, person)
//          stubForPensions(nino, pensionIncomeSource)
//          stubForEmploymentIncome(nino, employmentIncomeSource)
//          stubForEmploymentIncome(nino, status = NotLive)
//          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//          taxAccountSummaryIsFound(nino, taxAccountSummary)
//          taxAccountSummaryNotFound(nino, cyPlusone = true)
//          taxCalcValidResponse(nino, currentTaxYear, amount, Overpaid, repaymentStatus, time)
//          stubForBenefits(nino, noBenefits)
//          stubForTaxCodeChangeExists(nino)
//          stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//          val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//          response.status        shouldBe 200
//          response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//        }
//    }
//
//    "return OK with P800Repayments for some other date format" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      val time = LocalDate.now
//
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcWithInstantDate(nino, currentTaxYear, time)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status                                                shouldBe 200
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment shouldBe a[Some[_]]
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment.foreach { repayment =>
//        repayment.datePaid shouldBe a[Some[_]]
//        repayment.datePaid.foreach(l => l shouldBe time)
//      }
//    }
//
//    "return GONE when person is deceased" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person.copy(deceased = true))
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status shouldBe 410
//
//      nonTaxCodeIncomeNotCalled(nino)
//      taxAccountSummaryNotCalled(nino)
//      taxCalcCalled(nino, currentTaxYear)
//    }
//
//    "return LOCKED when person data locked in CID" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalLocked(nino)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status shouldBe 423
//
//      employmentsNotCalled(nino)
//      pensionsNotCalled(nino)
//      nonTaxCodeIncomeNotCalled(nino)
//      taxAccountSummaryNotCalled(nino)
//      taxCalcCalled(nino, currentTaxYear)
//    }
//
//    "return OK and a full valid MobilePayeResponse json ith employment benefit info" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, allBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status shouldBe 200
//
//      val result = Json.parse(response.body).as[MobilePayeSummaryResponse]
//
//      val employment1 = result.employments.get.head
//      val employment2 = result.employments.get.last
//      employment1.employmentBenefits.get.benefits.size shouldBe 3
//      employment1.employmentBenefits.get.benefits
//        .filter(_.benefitType.toString == CarBenefit.toString)
//        .head
//        .amount shouldBe BigDecimal(20000)
//      employment1.employmentBenefits.get.benefits
//        .filter(_.benefitType.toString == MedicalInsurance.toString)
//        .head
//        .amount shouldBe BigDecimal(650)
//      employment1.employmentBenefits.get.benefits
//        .filter(_.benefitType.toString == OtherBenefits.toString)
//        .head
//        .amount                                        shouldBe BigDecimal(450)
//      employment2.employmentBenefits.get.benefits.size shouldBe 2
//      employment2.employmentBenefits.get.benefits
//        .filter(_.benefitType.toString == MedicalInsurance.toString)
//        .head
//        .amount shouldBe BigDecimal(350)
//      employment2.employmentBenefits.get.benefits
//        .filter(_.benefitType.toString == OtherBenefits.toString)
//        .head
//        .amount shouldBe BigDecimal(100)
//    }
//
//    "return matching payloads when called with the current year as int and as 'current' " in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status shouldBe 200
//      val response2 = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
//      response2.status shouldBe 200
//
//      response.body[JsValue] shouldBe response2.body[JsValue]
//    }
//
//    "return OK and a full valid MobilePayeResponse json when no P800" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcWithNoP800(nino, currentTaxYear, LocalDate.now)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//    }
//
//    "return OK and no P800 when datePaid is more than 6 weeks ago" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now.minusWeeks(6).minusDays(1))
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//    }
//
//    "return OK and a P800 when datePaid is less than 6 weeks ago" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now.minusWeeks(6))
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status                                                shouldBe 200
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment shouldBe a[Some[_]]
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment.foreach { repayment =>
//        repayment.datePaid shouldBe a[Some[_]]
//        repayment.datePaid.foreach(l => l shouldBe LocalDate.now.minusWeeks(6))
//      }
//    }
//
//    "return OK and a P800 when datePaid is not present" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcWithNoDate(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status                                                shouldBe 200
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment shouldBe a[Some[_]]
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment.foreach { repayment =>
//        repayment.amount shouldBe a[Some[_]]
//        repayment.amount.foreach(l => l shouldBe 1000)
//      }
//    }
//
//    "return OK and no P800 when type is underpaid" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status                                    = NotLive)
//      taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoType = "underpaid")
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//    }
//
//    "return OK and no P800 when type is not overpaid" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status                                    = NotLive)
//      taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoType = "balanced")
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//    }
//
//    "return OK and no P800 when status is sa_user" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status                                      = NotLive)
//      taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "sa_user")
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//    }
//
//    "return OK and no P800 when status is unable_to_claim" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status                                      = NotLive)
//      taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "unable_to_claim")
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//    }
//
//    "return OK and a P800 with link when status is refund" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status                                      = NotLive)
//      taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "refund")
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status                                                shouldBe 200
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment shouldBe a[Some[_]]
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment.foreach { repayment =>
//        repayment.claimRefundLink shouldBe a[Some[_]]
//        repayment.claimRefundLink
//          .foreach(l => l shouldBe s"/tax-you-paid/${currentTaxYear - 1}-$currentTaxYear/paid-too-much")
//      }
//    }
//
//    "Call taxcalc for P800 repayments if a repayment was found on a call less than 1 day ago" in {
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      taxCalcWithNoDate(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response.status                                                shouldBe 200
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment shouldBe a[Some[_]]
//      response.body[JsValue].as[MobilePayeSummaryResponse].repayment.foreach { repayment =>
//        repayment.amount shouldBe a[Some[_]]
//        repayment.amount.foreach(l => l shouldBe 1000)
//      }
//
//      val response2 = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//      response2.status                                                shouldBe 200
//      response2.body[JsValue].as[MobilePayeSummaryResponse].repayment shouldBe a[Some[_]]
//      response2.body[JsValue].as[MobilePayeSummaryResponse].repayment.foreach { repayment =>
//        repayment.amount shouldBe a[Some[_]]
//        repayment.amount.foreach(l => l shouldBe 1000)
//      }
//      taxCalcCalled(nino, currentTaxYear, 2)
//    }
//
//    "Assume unshuttered and return OK and a full valid MobilePayeResponse json if error returned from mobile-shuttering service" in {
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status = NotLive)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      taxCalcNoResponse(nino, currentTaxYear)
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
//
//      response.status        shouldBe 200
//      response.body[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//
//    }
//  }
//
//  s"GET /nino/$nino/income-tax-history" should {
//
//    "return OK and a full valid income tax history json" in {
//      stubForShutteringDisabled("mobile-paye-income-tax-history")
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear, Seq(taxCodeIncome))
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 1, Seq(taxCodeIncome, taxCodeIncome2))
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 2, Seq(taxCodeIncome, taxCodeIncome2))
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 3, Seq(taxCodeIncome2, taxCodeIncome3))
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 4, Seq(taxCodeIncome2, taxCodeIncome3))
//      stubForEmploymentsOnly(nino, TaxYear.current.startYear, Seq(taiEmployment(TaxYear.current.startYear).copy(annualAccounts = Seq.empty)))
//      stubForAnnualAccounts(nino, TaxYear.current.startYear, Seq(annualAccount(TaxYear.current.startYear, Available, 3)))
//
//      stubForEmploymentsOnly(nino, TaxYear.current.startYear - 1, Seq(taiEmployment(TaxYear.current.startYear - 1).copy(annualAccounts = Seq.empty)))
//      stubForAnnualAccounts(nino, TaxYear.current.startYear - 1, Seq(annualAccount(TaxYear.current.startYear - 1, Available, 3)))
//
//      stubForEmploymentsOnly(
//        nino,
//        TaxYear.current.startYear - 2,
//        Seq(taiEmployment(TaxYear.current.startYear - 2).copy(annualAccounts = Seq.empty), taiEmployment2.copy(annualAccounts = Seq.empty))
//      )
//      stubForAnnualAccounts(nino, TaxYear.current.startYear - 2, Seq(annualAccount(TaxYear.current.startYear - 2, Available, 3)) ++ annualAccounts2)
//
//      stubForEmploymentsOnly(nino, TaxYear.current.startYear - 3, Seq(taiEmployment2.copy(annualAccounts = Seq.empty)))
//      stubForAnnualAccounts(nino, TaxYear.current.startYear - 3, annualAccounts2)
//
//      stubForEmploymentsOnly(nino,
//                             TaxYear.current.startYear - 4,
//                             Seq(taiEmployment2.copy(annualAccounts = Seq.empty), taiEmployment3.copy(annualAccounts = Seq.empty))
//                            )
//      stubForAnnualAccounts(nino, TaxYear.current.startYear - 4, annualAccounts2 ++ Seq(annualAccount(LocalDate.now().getYear, Available, 5)))
//
//      val response = await(getRequestWithAuthHeaders(incomeTaxHistoryUrl))
//      response.status                                   shouldBe 200
//      Json.parse(response.body).as[List[IncomeTaxYear]] shouldBe fullIncomeTaxHistoryList
//    }
//
//    "return OK and None  income tax history if parsing error  json" in {
//      stubForShutteringDisabled("mobile-paye-income-tax-history")
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForErrorJsonTaxCodeIncomes(nino, TaxYear.current.startYear, Seq.empty)
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 1, Seq(taxCodeIncome, taxCodeIncome2))
//      stubForEmploymentsOnlyJsonError(nino, TaxYear.current.startYear, Seq.empty)
//      stubForAnnualAccounts(nino, TaxYear.current.startYear, Seq.empty)
//
//      stubForEmploymentsOnly(nino, TaxYear.current.startYear - 1, Seq(taiEmployment(TaxYear.current.startYear - 1)))
//      stubForAnnualAccounts(nino, TaxYear.current.startYear - 1, Seq(annualAccount(TaxYear.current.startYear - 1, Available, 3)))
//
//      val response = await(getRequestWithAuthHeaders(incomeTaxHistoryUrl))
//      response.status                                   shouldBe 200
//      Json.parse(response.body).as[List[IncomeTaxYear]] shouldBe fullIncomeTaxHistoryListParsingError
//    }
//
//    "return OK and a empty valid income tax history json" in {
//      stubForShutteringDisabled("mobile-paye-income-tax-history")
//      grantAccess(nino)
//      personalDetailsAreFound(nino, person)
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear, Seq.empty)
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 1, Seq.empty)
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 2, Seq.empty)
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 3, Seq.empty)
//      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 4, Seq.empty)
//
//      val response = await(getRequestWithAuthHeaders(incomeTaxHistoryUrl))
//      response.status                                   shouldBe 200
//      Json.parse(response.body).as[List[IncomeTaxYear]] shouldBe emptyIncomeTaxHistoryList
//    }
//  }
//
//  s"GET /nino/$nino/previous-tax-year/$previousTaxYear/summary" should {
//    "return OK and a full valid MobilePayeResponse json" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      stubForEmploymentsOnly(nino, previousTaxYear, employmentOnlyData)
//      stubForAnnualAccounts(nino, previousTaxYear, annualAccountsIt)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome, previousTaxYear)
//      taxAccountSummaryIsFound(nino, taxAccountSummary, taxYear = previousTaxYear)
//      stubForBenefits(nino, noBenefits, previousTaxYear)
//      stubForTaxCodes(nino, previousTaxYear, taxCodeData)
//
//      val response = await(getRequestWithAuthHeaders(urlWithPreviousYear))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json
//        .toJson(fullMobilePayePreviousYearResponse())
//
//    }
//
//    "return OK and a valid MobilePayeResponse json without employments" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      stubForEmploymentsOnly(nino,
//                             previousTaxYear,
//                             Seq(taiEmployment3.copy(name = "ALDI", employmentType = PensionIncome, annualAccounts = Seq.empty))
//                            )
//      stubForAnnualAccounts(nino, previousTaxYear, Seq(annualAccount(LocalDate.now().getYear, Available, seqNo = 5)))
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome, previousTaxYear)
//      taxAccountSummaryIsFound(nino, taxAccountSummary, taxYear = previousTaxYear)
//      stubForBenefits(nino, noBenefits, previousTaxYear)
//      stubForTaxCodes(nino, previousTaxYear, taxCodeData)
//
//      val response = await(getRequestWithAuthHeaders(urlWithPreviousYear))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json
//        .toJson(fullMobilePayePreviousYearResponse().copy(employments = None, previousEmployments = None))
//
//    }
//
//    "return OK and a valid MobilePayeResponse json without pensions" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      stubForEmploymentsOnly(
//        nino,
//        previousTaxYear,
//        Seq(
//          taiEmployment(TaxYear.current.previous.startYear).copy(annualAccounts = Seq.empty),
//          taiEmployment2.copy(employmentStatus                                  = Ceased, annualAccounts = Seq.empty)
//        )
//      )
//      stubForAnnualAccounts(
//        nino,
//        previousTaxYear,
//        Seq(
//          annualAccount(TaxYear.current.previous.startYear, Available, seqNo = 3),
//          AnnualAccount(4,
//                        TaxYear(startYear = TaxYear.current.back(4).currentYear),
//                        Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)),
//                        Available
//                       ),
//          AnnualAccount(4, TaxYear(TaxYear.current.back(5).currentYear), Seq(Payment(LocalDate.now().minusDays(63), 50, 20, 10, 30, 5, 2)), Available)
//        )
//      )
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome, previousTaxYear)
//      taxAccountSummaryIsFound(nino, taxAccountSummary, taxYear = previousTaxYear)
//      stubForBenefits(nino, noBenefits, previousTaxYear)
//      stubForTaxCodes(nino, previousTaxYear, taxCodeData)
//
//      val response = await(getRequestWithAuthHeaders(urlWithPreviousYear))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json
//        .toJson(fullMobilePayePreviousYearResponse().copy(pensions = None))
//
//    }
//
//    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      stubForEmploymentsOnly(nino, previousTaxYear, employmentOnlyData)
//      stubForAnnualAccounts(nino, previousTaxYear, annualAccountsIt)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil), previousTaxYear)
//      taxAccountSummaryIsFound(nino, taxAccountSummary, taxYear = previousTaxYear)
//      stubForBenefits(nino, noBenefits, previousTaxYear)
//      stubForTaxCodes(nino, previousTaxYear, taxCodeData)
//
//      val response = await(getRequestWithAuthHeaders(urlWithPreviousYear))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json
//        .toJson(fullMobilePayePreviousYearResponse().copy(otherIncomes = None))
//
//    }
//
//    "return 404 when tax year supplied is beyond the history limit" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//
//      val response = await(
//        getRequestWithAuthHeaders(
//          s"/nino/$nino/previous-tax-year/${previousTaxYear - 1}/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//        )
//      )
//      response.status shouldBe 404
//    }
//
//    "return 400 when no journeyId supplied" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//
//      val response =
//        await(wsUrl(s"/nino/$nino/previous-tax-year/$previousTaxYear/summary").addHttpHeaders(acceptJsonHeader).get())
//      response.status shouldBe 400
//    }
//
//    "return 400 when invalid journeyId supplied" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//
//      val response = await(
//        wsUrl(s"/nino/$nino/previous-tax-year/$previousTaxYear/summary?journeyId=ThisIsAnInvalidJourneyId")
//          .addHttpHeaders(acceptJsonHeader)
//          .get()
//      )
//      response.status shouldBe 400
//    }
//  }
//
//  s"GET /nino/$nino/tax-code" should {
//    "return OK and a the user's current tax code" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(getRequestWithAuthHeaders(taxCodeUrl))
//      response.status shouldBe 200
//      response.body[JsValue] shouldBe Json
//        .obj("taxCode" -> taxCodeRecord.taxCode)
//
//    }
//
//    "return 404 when no tax codes found" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//      stubForTaxCodeChange(nino, TaxCodeChangeDetails(Seq.empty, Seq.empty))
//
//      val response = await(getRequestWithAuthHeaders(taxCodeUrl))
//      response.status shouldBe 404
//    }
//
//    "return 400 when no journeyId supplied" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//
//      val response = await(wsUrl(s"/nino/$nino/tax-code").addHttpHeaders(acceptJsonHeader).get())
//      response.status shouldBe 400
//    }
//
//    "return 400 when invalid journeyId supplied" in {
//      stubForShutteringDisabled()
//      grantAccess(nino)
//
//      val response = await(
//        wsUrl(s"/nino/$nino/tax-code?journeyId=ThisIsAnInvalidJourneyId")
//          .addHttpHeaders(acceptJsonHeader)
//          .get()
//      )
//      response.status shouldBe 400
//    }
//  }
//
//}
//
//class LiveMobilePayeControllerShutteredISpec extends BaseISpec {
//
//  val request = s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//
//  implicit def ninoToString(nino: Nino): String = nino.toString()
//
//  s"GET /nino/$nino/tax-year/$currentTaxYear/summary but SHUTTERED" should {
//
//    "return SHUTTERED when shuttered" in {
//      stubForShutteringEnabled()
//      grantAccess(nino)
//
//      val response: WSResponse = await(getRequestWithAuthHeaders(request))
//      response.status shouldBe 521
//      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
//      shuttering.shuttered shouldBe true
//      shuttering.title     shouldBe Some("Shuttered")
//      shuttering.message   shouldBe Some("PAYE is currently not available")
//
//      nonTaxCodeIncomeNotCalled(nino)
//      employmentsNotCalled(nino)
//      pensionsNotCalled(nino)
//      taxAccountSummaryNotCalled(nino)
//      taxCalcCalled(nino, currentTaxYear)
//    }
//  }
//
//}
//
//class LiveMobilePayeControllerp800CacheEnabledISpec extends BaseISpec with Injecting with PlayMongoRepositorySupport[P800Cache] {
//
//  override val repository: PlayMongoRepository[P800Cache] = app.injector.instanceOf[P800CacheMongo]
//
//  override protected def appBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder().configure(
//    config ++
//      Map(
//        "p800CacheEnabled" -> true
//      )
//  )
//
//  override implicit lazy val app: Application = appBuilder.build()
//
//  implicit def ninoToString(nino: Nino): String = nino.toString()
//
//  s"GET /nino/$nino/tax-year/$currentTaxYear/summary" should {
//
//    "Not call taxcalc for P800 repayments if no repayment was found on a call less than 1 day ago" in {
//
//      dropCollection()
//
//      when(mockFeatureFlagService.get(any()))
//        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))
//      grantAccess(nino)
//      stubForShutteringDisabled()
//      personalDetailsAreFound(nino, person)
//      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
//      taxAccountSummaryIsFound(nino, taxAccountSummary)
//      taxAccountSummaryNotFound(nino, cyPlusone = true)
//      stubForPensions(nino, pensionIncomeSource)
//      stubForEmploymentIncome(nino, employmentIncomeSource)
//      stubForEmploymentIncome(nino, status                                                  = NotLive)
//      taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now.minusYears(1), yearTwoType = "balanced")
//      stubForBenefits(nino, noBenefits)
//      stubForTaxCodeChangeExists(nino)
//      stubForTaxCodeChange(nino, taxCodeChangeDetails)
//
//      val response = await(
//        getRequestWithAuthHeaders(
//          s"/nino/$nino/tax-year/current/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//        )
//      )
//
//      response.status                       shouldBe 200
//      Json.parse(response.body).as[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//
//      Thread.sleep(2000)
//
//      val response2 = await(
//        getRequestWithAuthHeaders(
//          s"/nino/$nino/tax-year/current/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"
//        )
//      )
//      response2.status                      shouldBe 200
//      Json.parse(response.body).as[JsValue] shouldBe Json.toJson(fullMobilePayeResponse)
//
//      dropCollection()
//    }
//  }
//
//}
