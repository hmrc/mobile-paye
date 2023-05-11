import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Injecting
import stubs.AuthStub._
import stubs.ShutteringStub._
import stubs.TaiStub._
import stubs.TaxCalcStub._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, OnlinePaymentIntegration}
import uk.gov.hmrc.mobilepaye.config.MobilePayeConfig
import uk.gov.hmrc.mobilepaye.domain.tai.{CarBenefit, MedicalInsurance}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status.{Overpaid, Underpaid}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus._
import uk.gov.hmrc.mobilepaye.domain.{IncomeTaxYear, MobilePayeResponse, OtherBenefits, P800Cache, P800Repayment, Shuttering}
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import uk.gov.hmrc.time.TaxYear
import utils.BaseISpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

class LiveMobilePayeControllerISpec extends BaseISpec with Injecting with PlayMongoRepositorySupport[P800Cache] {

  val appConfig: MobilePayeConfig = MobilePayeConfig(app.configuration)

  override lazy val repository: PlayMongoRepository[P800Cache] = app.injector.instanceOf[P800CacheMongo]

  lazy val urlWithCurrentYearAsInt =
    s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"

  lazy val urlWithCurrentYearAsCurrent =
    s"/nino/$nino/tax-year/current/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"

  lazy val incomeTaxHistoryUrl =
    s"/nino/$nino/income-tax-history?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"

  override def beforeEach(): Unit = {
    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = false)))
    super.beforeEach()
  }
  implicit def ninoToString(nino: Nino): String = nino.toString()

  def dropDb = dropCollection()

  s"GET /nino/$nino/tax-year/$currentTaxYear/summary" should {
    "return OK and a full valid MobilePayeResponse json" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryIsFound(nino, taxAccountSummary, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponseWithCY1Link).toString()

    }

    "return OK and a full valid MobilePayeResponse json with no comparison link if data not found" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse).toString()

    }

    "return OK and a valid MobilePayeResponse json without untaxed income but other income" in {
      stubForShutteringDisabled
      grantAccess(nino)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      personalDetailsAreFound(nino, person)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(
        otherIncomes = Some(Seq(otherIncome))
      )
    }

    "return OK and a valid MobilePayeResponse json without employments" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, Seq.empty)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(employments = None)
    }

    "return OK and a valid MobilePayeResponse json without pensions" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, Seq.empty)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      stubForPensions(nino, pensionIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
    }

    "return GONE when person is deceased" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person.copy(isDeceased = true))

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status shouldBe 410

      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcCalled(nino, currentTaxYear)
    }

    "return LOCKED when person is locked in CID" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalLocked(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status shouldBe 423

      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcCalled(nino, currentTaxYear)
    }

    "return LOCKED when mci is returned true" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person.copy(manualCorrespondenceInd = Some(true)))

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
      response.status shouldBe 423
      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcCalled(nino, currentTaxYear)
    }

    "return 400 when no journeyId supplied" in {
      stubForShutteringDisabled
      grantAccess(nino)

      val response =
        await(wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary").addHttpHeaders(acceptJsonHeader).get())
      response.status shouldBe 400
    }

    "return 400 when invalid journeyId supplied" in {
      stubForShutteringDisabled
      grantAccess(nino)

      val response = await(
        wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=ThisIsAnInvalidJourneyId")
          .addHttpHeaders(acceptJsonHeader)
          .get()
      )
      response.status shouldBe 400
    }
  }

  s"GET /nino/$nino/tax-year/current/summary" should {

    "return OK and a full valid MobilePayeResponse json" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
    }

    "return OK and a valid MobilePayeResponse json without untaxed income but other income" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(
        otherIncomes = Some(Seq(otherIncome))
      )
    }

    "return OK and a valid MobilePayeResponse json without employments" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, Seq.empty)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(employments = None)
    }

    "return OK and a valid MobilePayeResponse json without pensions" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, Seq.empty)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
    }

    "return OK with P800Repayments for Overpaid tax and accepted RepaymentStatus" in {
      when(mockFeatureFlagService.get(any()))
        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))

      stubForShutteringDisabled
      dropDb
      List(Refund, PaymentProcessing, PaymentPaid, ChequeSent)
        .foreach { repaymentStatus =>
          val amount = Random.nextDouble(): BigDecimal
          val time   = LocalDate.now

          grantAccess(nino)
          personalDetailsAreFound(nino, person)
          stubForPensions(nino, pensionIncomeSource)
          stubForEmploymentIncome(nino, employmentIncomeSource)
          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
          taxAccountSummaryIsFound(nino, taxAccountSummary)
          taxAccountSummaryNotFound(nino, cyPlusone = true)
          taxCalcValidResponse(nino, currentTaxYear - 1, amount, Overpaid, repaymentStatus, time)
          stubForBenefits(nino, noBenefits)
          stubForTaxCodeChangeExists(nino)

          val expectedRepayment: Option[P800Repayment] =
            repayment(P800Status.Overpaid, repaymentStatus, currentTaxYear, amount, time)

          val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
          response.status shouldBe 200
          response.body[JsValue].as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(
            repayment = expectedRepayment
          )
        }
    }

    "return OK with no P800Repayments for Underpaid tax" in {
      stubForShutteringDisabled
      List(PaymentDue, PartPaid, PaidAll, PaymentsDown, Unknown)
        .foreach { repaymentStatus =>
          val amount = Random.nextDouble(): BigDecimal
          val time   = LocalDate.now

          grantAccess(nino)
          personalDetailsAreFound(nino, person)
          stubForPensions(nino, pensionIncomeSource)
          stubForEmploymentIncome(nino, employmentIncomeSource)
          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
          taxAccountSummaryIsFound(nino, taxAccountSummary)
          taxAccountSummaryNotFound(nino, cyPlusone = true)
          taxCalcValidResponse(nino, currentTaxYear, amount, Underpaid, repaymentStatus, time)
          stubForBenefits(nino, noBenefits)
          stubForTaxCodeChangeExists(nino)

          val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
          response.status                               shouldBe 200
          response.body[JsValue].as[MobilePayeResponse] shouldBe fullMobilePayeResponse
        }
    }

    "return OK with no P800Repayments for uncovered RepaymentStatuses" in {
      stubForShutteringDisabled
      List(SaUser, UnableToClaim)
        .foreach { repaymentStatus =>
          val amount = Random.nextDouble(): BigDecimal
          val time   = LocalDate.now

          grantAccess(nino)
          personalDetailsAreFound(nino, person)
          stubForPensions(nino, pensionIncomeSource)
          stubForEmploymentIncome(nino, employmentIncomeSource)
          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
          taxAccountSummaryIsFound(nino, taxAccountSummary)
          taxAccountSummaryNotFound(nino, cyPlusone = true)
          taxCalcValidResponse(nino, currentTaxYear, amount, Overpaid, repaymentStatus, time)
          stubForBenefits(nino, noBenefits)
          stubForTaxCodeChangeExists(nino)

          val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
          response.status                               shouldBe 200
          response.body[JsValue].as[MobilePayeResponse] shouldBe fullMobilePayeResponse
        }
    }

    "return OK with P800Repayments for some other date format" in {
      when(mockFeatureFlagService.get(any()))
        .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))

      stubForShutteringDisabled
      dropDb
      val time = LocalDate.now

      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      taxCalcWithInstantDate(nino, currentTaxYear, time)
      stubForBenefits(nino, noBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status                                         shouldBe 200
      response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
      response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
        repayment.datePaid shouldBe a[Some[_]]
        repayment.datePaid.foreach(l => l shouldBe time)
      }
    }

    "return GONE when person is deceased" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person.copy(isDeceased = true))

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status shouldBe 410

      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcCalled(nino, currentTaxYear)
    }

    "return LOCKED when person data locked in CID" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalLocked(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status shouldBe 423

      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcCalled(nino, currentTaxYear)
    }

    "return LOCKED when mci set to true" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person.copy(manualCorrespondenceInd = Some(true)))

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status shouldBe 423
      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcCalled(nino, currentTaxYear)
    }

    "return TOO_MANY_REQUESTS when the TAI returns a 429" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalTooManyRequests(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status shouldBe 429

    }

    "return OK and a full valid MobilePayeResponse json ith employment benefit info" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxAccountSummaryNotFound(nino, cyPlusone = true)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmploymentIncome(nino, employmentIncomeSource)
      taxCalcNoResponse(nino, currentTaxYear)
      stubForBenefits(nino, allBenefits)
      stubForTaxCodeChangeExists(nino)

      val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
      response.status shouldBe 200

      val result = Json.parse(response.body).as[MobilePayeResponse]

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

  }

  "return matching payloads when called with the current year as int and as 'current' " in {
    stubForShutteringDisabled
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcNoResponse(nino, currentTaxYear)
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status shouldBe 200
    val response2 = await(getRequestWithAuthHeaders(urlWithCurrentYearAsInt))
    response2.status shouldBe 200

    response.body shouldBe response2.body
  }

  "return OK and a full valid MobilePayeResponse json when no P800" in {
    stubForShutteringDisabled
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithNoP800(nino, currentTaxYear, LocalDate.now)
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and no P800 when datePaid is more than 6 weeks ago" in {
    stubForShutteringDisabled
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now.minusWeeks(6).minusDays(1))
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and a P800 when datePaid is less than 6 weeks ago" in {
    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))

    stubForShutteringDisabled
    dropDb
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now.minusWeeks(6))
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                         shouldBe 200
    response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
    response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
      repayment.datePaid shouldBe a[Some[_]]
      repayment.datePaid.foreach(l => l shouldBe LocalDate.now.minusWeeks(6))
    }
  }

  "return OK and a P800 when datePaid is not present" in {
    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))

    stubForShutteringDisabled
    dropDb
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithNoDate(nino, currentTaxYear)
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                         shouldBe 200
    response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
    response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
      repayment.amount shouldBe a[Some[_]]
      repayment.amount.foreach(l => l shouldBe 1000)
    }
  }

  "return OK and no P800 when type is underpaid" in {
    stubForShutteringDisabled
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoType = "underpaid")
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and no P800 when type is not overpaid" in {
    stubForShutteringDisabled
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoType = "balanced")
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and no P800 when status is sa_user" in {
    stubForShutteringDisabled
    dropDb
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "sa_user")
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and no P800 when status is unable_to_claim" in {
    stubForShutteringDisabled
    dropDb
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "unable_to_claim")
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and a P800 with link when status is refund" in {
    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))

    stubForShutteringDisabled
    dropDb
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "refund")
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                         shouldBe 200
    response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
    response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
      repayment.claimRefundLink shouldBe a[Some[_]]
      repayment.claimRefundLink
        .foreach(l => l shouldBe s"/tax-you-paid/${currentTaxYear - 1}-$currentTaxYear/paid-too-much")
    }
  }

  "Do not call taxcalc for P800 repayments if no repayment was found on a call less than 1 day ago" in {
    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))

    stubForShutteringDisabled
    dropDb
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoType = "underpaid")
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse

    val response2 = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response2.status                                  shouldBe 200
    Json.parse(response2.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse

    taxCalcCalled(nino, currentTaxYear, 1)
  }

  "Call taxcalc for P800 repayments if a repayment was found on a call less than 1 day ago" in {
    when(mockFeatureFlagService.get(any()))
      .thenReturn(Future.successful(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)))

    stubForShutteringDisabled
    dropDb
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    taxCalcWithNoDate(nino, currentTaxYear)
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status                                         shouldBe 200
    response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
    response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
      repayment.amount shouldBe a[Some[_]]
      repayment.amount.foreach(l => l shouldBe 1000)
    }

    val response2 = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response2.status                                         shouldBe 200
    response2.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
    response2.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
      repayment.amount shouldBe a[Some[_]]
      repayment.amount.foreach(l => l shouldBe 1000)
    }
    taxCalcCalled(nino, currentTaxYear, 2)
  }

  "Assume unshuttered and return OK and a full valid MobilePayeResponse json if error returned from mobile-shuttering service" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmploymentIncome(nino, employmentIncomeSource)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    taxAccountSummaryNotFound(nino, cyPlusone = true)
    taxCalcNoResponse(nino, currentTaxYear)
    stubForBenefits(nino, noBenefits)
    stubForTaxCodeChangeExists(nino)

    val response = await(getRequestWithAuthHeaders(urlWithCurrentYearAsCurrent))
    response.status shouldBe 200
    response.body   shouldBe Json.toJson(fullMobilePayeResponse).toString()

  }

  s"GET /nino/$nino/income-tax-history" should {

    "return OK and a full valid income tax history json" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear, Seq(taxCodeIncome))
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 1, Seq(taxCodeIncome, taxCodeIncome2))
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 2, Seq(taxCodeIncome, taxCodeIncome2))
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 3, Seq(taxCodeIncome2, taxCodeIncome3))
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 4, Seq(taxCodeIncome2, taxCodeIncome3))
      stubForEmployments(nino, TaxYear.current.startYear, Seq(taiEmployment(TaxYear.current.startYear)))
      stubForEmployments(nino, TaxYear.current.startYear - 1, Seq(taiEmployment(TaxYear.current.startYear - 1)))
      stubForEmployments(nino,
                         TaxYear.current.startYear - 2,
                         Seq(taiEmployment(TaxYear.current.startYear - 2), taiEmployment2))
      stubForEmployments(nino, TaxYear.current.startYear - 3, Seq(taiEmployment2))
      stubForEmployments(nino, TaxYear.current.startYear - 4, Seq(taiEmployment2, taiEmployment3))

      val response = await(getRequestWithAuthHeaders(incomeTaxHistoryUrl))
      response.status                                   shouldBe 200
      Json.parse(response.body).as[List[IncomeTaxYear]] shouldBe fullIncomeTaxHistoryList
    }

    "return OK and a empty valid income tax history json" in {
      stubForShutteringDisabled
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear, Seq.empty)
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 1, Seq.empty)
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 2, Seq.empty)
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 3, Seq.empty)
      stubForTaxCodeIncomes(nino, TaxYear.current.startYear - 4, Seq.empty)

      val response = await(getRequestWithAuthHeaders(incomeTaxHistoryUrl))
      response.status                                   shouldBe 200
      Json.parse(response.body).as[List[IncomeTaxYear]] shouldBe emptyIncomeTaxHistoryList
    }
  }

}

class LiveMobilePayeControllerShutteredISpec extends BaseISpec {

  val request = s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0"

  implicit def ninoToString(nino: Nino): String = nino.toString()

  s"GET /nino/$nino/tax-year/$currentTaxYear/summary but SHUTTERED" should {

    "return SHUTTERED when shuttered" in {
      stubForShutteringEnabled
      grantAccess(nino)

      val response: WSResponse = await(getRequestWithAuthHeaders(request))
      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe Some("Shuttered")
      shuttering.message   shouldBe Some("PAYE is currently not available")

      nonTaxCodeIncomeNotCalled(nino)
      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcCalled(nino, currentTaxYear)
    }
  }

}
