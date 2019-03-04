import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import stubs.AuthStub._
import stubs.TaiStub._
import uk.gov.hmrc.mobilepaye.domain.Shuttering
import uk.gov.hmrc.mobilepaye.domain.tai._
import utils.BaseISpec

class LiveMobilePayeControllerISpec extends BaseISpec {
  lazy val requestWithCurrentYearAsInt:            WSRequest = wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=12345").addHttpHeaders(acceptJsonHeader)
  lazy val requestWithCurrentYearAsCurrent:            WSRequest = wsUrl(s"/nino/$nino/tax-year/current/summary?journeyId=12345").addHttpHeaders(acceptJsonHeader)

  override def shuttered: Boolean   = false

  s"GET /nino/$nino/tax-year/$currentTaxYear/summary" should {

    "return OK and a full valid MobilePayeResponse json" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse).toString()
    }

    "return OK and a valid MobilePayeResponse json without untaxed income but other income" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = Some(Seq(otherIncome)))).toString()
    }

    "return OK and a valid MobilePayeResponse json without employments" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes.filter(_.componentType == PensionIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse.copy(employments = None)).toString()
    }

    "return OK and a valid MobilePayeResponse json without pensions" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes.filter(_.componentType == EmploymentIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse.copy(pensions = None)).toString()
    }

    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = None)).toString()
    }

    "return GONE when person is deceased" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person.copy(isDeceased = true))

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 410

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }

    "return 423 when person is locked in CID" in {
      grantAccess(nino.toString)
      personalLocked(nino.toString)

      val response = await(requestWithCurrentYearAsInt.get())
      response.status shouldBe 423

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }
  }

  s"GET /nino/$nino/tax-year/current/summary" should {

    "return OK and a full valid MobilePayeResponse json" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse).toString()
    }

    "return OK and a valid MobilePayeResponse json without untaxed income but other income" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = Some(Seq(otherIncome)))).toString()
    }

    "return OK and a valid MobilePayeResponse json without employments" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes.filter(_.componentType == PensionIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse.copy(employments = None)).toString()
    }

    "return OK and a valid MobilePayeResponse json without pensions" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes.filter(_.componentType == EmploymentIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse.copy(pensions = None)).toString()
    }

    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 200
      response.body   shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = None)).toString()
    }

    "return GONE when person is deceased" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person.copy(isDeceased = true))

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 410

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }

    "return 423 when person data locked in CID" in {
      grantAccess(nino.toString)
      personalLocked(nino.toString)

      val response = await(requestWithCurrentYearAsCurrent.get())
      response.status shouldBe 423

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }
  }

  "return matching payloads when called with the current year as int and as 'current' " in {
    grantAccess(nino.toString)
    personalDetailsAreFound(nino.toString, person)
    taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
    nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
    employmentsAreFound(nino.toString(), taiEmployments)
    taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

    val response = await(requestWithCurrentYearAsCurrent.get())
    response.status shouldBe 200
    val response2 = await(requestWithCurrentYearAsInt.get())
    response2.status shouldBe 200

    response.body   shouldBe response2.body
  }
}

class LiveMobilePayeControllerShutteredISpec extends BaseISpec {
  val request:            WSRequest = wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=12345").addHttpHeaders(acceptJsonHeader)
  override def shuttered: Boolean   = true

  s"GET /nino/$nino/tax-year/$currentTaxYear/summary but SHUTTERED" should {

    "return SHUTTERED when shuttered" in {
      grantAccess(nino.toString)

      val response: WSResponse = await(request.get())
      response.status shouldBe 521
      val shuttering: Shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title     shouldBe "Shuttered"
      shuttering.message   shouldBe "PAYE is currently not available"

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }
  }
}
