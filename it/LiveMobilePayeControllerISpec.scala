import java.time.{LocalDate, Month}

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import stubs.AuthStub._
import stubs.TaiStub._
import stubs.TaxCalcStub._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status.{Overpaid, Underpaid}
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus._
import uk.gov.hmrc.mobilepaye.domain.{MobilePayeResponse, P800Repayment, Shuttering}
import utils.BaseISpec

import scala.util.Random

class LiveMobilePayeControllerISpec extends BaseISpec {
  lazy val requestWithCurrentYearAsInt: WSRequest =
    wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=12345").addHttpHeaders(acceptJsonHeader)
  lazy val requestWithCurrentYearAsCurrent: WSRequest =
    wsUrl(s"/nino/$nino/tax-year/current/summary?journeyId=12345").addHttpHeaders(acceptJsonHeader)

  implicit def ninoToString(nino: Nino): String = nino.toString()

  override def shuttered: Boolean = false

  s"GET /nino/$nino/tax-year/$currentTaxYear/summary" should {
    "return OK and a full valid MobilePayeResponse json" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmployments(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse).toString()
    }

    "return OK and a valid MobilePayeResponse json without untaxed income but other income" in {
      grantAccess(nino)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmployments(nino, employmentIncomeSource)
      personalDetailsAreFound(nino, person)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(otherIncomes = Some(Seq(otherIncome)))
    }

    "return OK and a valid MobilePayeResponse json without employments" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmployments(nino, Seq.empty)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(employments = None)
    }

    "return OK and a valid MobilePayeResponse json without pensions" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, Seq.empty)
      stubForEmployments(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForEmployments(nino, employmentIncomeSource)
      stubForPensions(nino, pensionIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
    }

    "return GONE when person is deceased" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person.copy(isDeceased = true))

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 410

      taxCodeIncomeNotCalled(nino)
      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcNotCalled(nino, currentTaxYear)
    }

    "return 423 when person is locked in CID" in {
      grantAccess(nino)
      personalLocked(nino)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 423

      taxCodeIncomeNotCalled(nino)
      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcNotCalled(nino, currentTaxYear)
    }
  }

  s"GET /nino/$nino/tax-year/current/summary" should {

    "return OK and a full valid MobilePayeResponse json" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmployments(nino, employmentIncomeSource)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
    }

    "return OK and a valid MobilePayeResponse json without untaxed income but other income" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmployments(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(otherIncomes = Some(Seq(otherIncome)))
    }

    "return OK and a valid MobilePayeResponse json without employments" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmployments(nino, Seq.empty)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(employments = None)
    }

    "return OK and a valid MobilePayeResponse json without pensions" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, Seq.empty)
      stubForEmployments(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(pensions = None)
    }

    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmployments(nino, employmentIncomeSource)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcNoResponse(nino, currentTaxYear)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status                                  shouldBe 200
      Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(otherIncomes = None)
    }

    "return OK with P800Repayments for Overpaid tax and accepted RepaymentStatus" in {
      List(Refund, PaymentProcessing, PaymentPaid, ChequeSent)
        .foreach { repaymentStatus =>
          val amount = Random.nextDouble(): BigDecimal
          val time   = LocalDate.now

          grantAccess(nino)
          personalDetailsAreFound(nino, person)
          stubForPensions(nino, pensionIncomeSource)
          stubForEmployments(nino, employmentIncomeSource)
          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
          taxAccountSummaryIsFound(nino, taxAccountSummary)
          taxCalcValidResponse(nino, currentTaxYear - 1, amount, Overpaid, repaymentStatus, time)

          val expectedRepayment: Option[P800Repayment] = repayment(P800Status.Overpaid, repaymentStatus, currentTaxYear, amount, time)

          val response = await(requestWithCurrentYearAsCurrent.get())
          response.status                               shouldBe 200
          response.body[JsValue].as[MobilePayeResponse] shouldBe fullMobilePayeResponse.copy(repayment = expectedRepayment)
        }
    }

    "return OK with no P800Repayments for Underpaid tax" in {
      List(PaymentDue, PartPaid, PaidAll, PaymentsDown, Unknown)
        .foreach { repaymentStatus =>
          val amount = Random.nextDouble(): BigDecimal
          val time   = LocalDate.now

          grantAccess(nino)
          personalDetailsAreFound(nino, person)
          stubForPensions(nino, pensionIncomeSource)
          stubForEmployments(nino, employmentIncomeSource)
          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
          taxAccountSummaryIsFound(nino, taxAccountSummary)
          taxCalcValidResponse(nino, currentTaxYear, amount, Underpaid, repaymentStatus, time)

          val response = await(requestWithCurrentYearAsCurrent.get())
          response.status                               shouldBe 200
          response.body[JsValue].as[MobilePayeResponse] shouldBe fullMobilePayeResponse
        }
    }

    "return OK with no P800Repayments for uncovered RepaymentStatuses" in {
      List(SaUser, UnableToClaim)
        .foreach { repaymentStatus =>
          val amount = Random.nextDouble(): BigDecimal
          val time   = LocalDate.now

          grantAccess(nino)
          personalDetailsAreFound(nino, person)
          stubForPensions(nino, pensionIncomeSource)
          stubForEmployments(nino, employmentIncomeSource)
          nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
          taxAccountSummaryIsFound(nino, taxAccountSummary)
          taxCalcValidResponse(nino, currentTaxYear, amount, Overpaid, repaymentStatus, time)

          val response = await(requestWithCurrentYearAsCurrent.get())
          response.status                               shouldBe 200
          response.body[JsValue].as[MobilePayeResponse] shouldBe fullMobilePayeResponse
        }
    }

    "return OK with P800Repayments for some other date format" in {
      val time = LocalDate.now

      grantAccess(nino)
      personalDetailsAreFound(nino, person)
      nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
      stubForPensions(nino, pensionIncomeSource)
      stubForEmployments(nino, employmentIncomeSource)
      taxAccountSummaryIsFound(nino, taxAccountSummary)
      taxCalcWithInstantDate(nino, currentTaxYear, time)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status                                         shouldBe 200
      response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
      response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
        repayment.datePaid shouldBe a[Some[_]]
        repayment.datePaid.foreach(l => l shouldBe time)
      }
    }

    "return GONE when person is deceased" in {
      grantAccess(nino)
      personalDetailsAreFound(nino, person.copy(isDeceased = true))

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 410

      taxCodeIncomeNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcNotCalled(nino, currentTaxYear)
    }

    "return 423 when person data locked in CID" in {
      grantAccess(nino)
      personalLocked(nino)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 423

      taxCodeIncomeNotCalled(nino)
      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcNotCalled(nino, currentTaxYear)
    }
  }

  "return matching payloads when called with the current year as int and as 'current' " in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcNoResponse(nino, currentTaxYear)

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status shouldBe 200
    val response2 = await(requestWithCurrentYearAsInt.get())
    response2.status shouldBe 200

    response.body shouldBe response2.body
  }

  "return OK and a full valid MobilePayeResponse json when no P800" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcWithNoP800(nino, currentTaxYear, LocalDate.now)

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and no P800 when datePaid is more than 6 weeks ago" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now.minusWeeks(6).minusDays(1))

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and a P800 when datePaid is less than 6 weeks ago" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now.minusWeeks(6))

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status                                         shouldBe 200
    response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
    response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
      repayment.datePaid shouldBe a[Some[_]]
      repayment.datePaid.foreach(l => l shouldBe LocalDate.now.minusWeeks(6))
    }
  }

  "return OK and a P800 when datePaid is not present" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcWithNoDate(nino, currentTaxYear)

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status                                         shouldBe 200
    response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
    response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
      repayment.amount shouldBe a[Some[_]]
      repayment.amount.foreach(l => l shouldBe 1000)
    }
  }

  "return OK and no P800 when type is not overpaid" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoType = "underpaid")

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and no P800 when status is sa_user" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "sa_user")

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and no P800 when status is unable_to_claim" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "unable_to_claim")

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status                                  shouldBe 200
    Json.parse(response.body).as[MobilePayeResponse] shouldBe fullMobilePayeResponse
  }

  "return OK and a P800 with link when status is refund" in {
    grantAccess(nino)
    personalDetailsAreFound(nino, person)
    nonTaxCodeIncomeIsFound(nino, nonTaxCodeIncome)
    taxAccountSummaryIsFound(nino, taxAccountSummary)
    stubForPensions(nino, pensionIncomeSource)
    stubForEmployments(nino, employmentIncomeSource)
    taxCalcWithInstantDate(nino, currentTaxYear, LocalDate.now, yearTwoStatus = "refund")

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status                                         shouldBe 200
    response.body[JsValue].as[MobilePayeResponse].repayment shouldBe a[Some[_]]
    response.body[JsValue].as[MobilePayeResponse].repayment.foreach { repayment =>
      repayment.claimRefundLink shouldBe a[Some[_]]
      repayment.claimRefundLink.foreach(l => l shouldBe s"/tax-you-paid/${LocalDate.now.getYear - 1}-${LocalDate.now.getYear}/paid-too-much")
    }
  }
}

class LiveMobilePayeControllerShutteredISpec extends BaseISpec {
  val request:            WSRequest = wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=12345").addHttpHeaders(acceptJsonHeader)
  override def shuttered: Boolean   = true
  implicit def ninoToString(nino: Nino): String = nino.toString()

  s"GET /nino/$nino/tax-year/$currentTaxYear/summary but SHUTTERED" should {

    "return SHUTTERED when shuttered" in {
      grantAccess(nino)

      val response: WSResponse = await(request.get())
      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe "Shuttered"
      shuttering.message   shouldBe "PAYE is currently not available"

      taxCodeIncomeNotCalled(nino)
      nonTaxCodeIncomeNotCalled(nino)
      employmentsNotCalled(nino)
      pensionsNotCalled(nino)
      taxAccountSummaryNotCalled(nino)
      taxCalcNotCalled(nino, currentTaxYear)
    }
  }
}
