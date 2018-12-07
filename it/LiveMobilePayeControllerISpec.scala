import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import stubs.AuthStub._
import stubs.TaiStub._
import uk.gov.hmrc.mobilepaye.domain.tai._
import utils.BaseISpec

class LiveMobilePayeControllerISpec extends BaseISpec {

  s"GET /$nino/summary/current-income" should {
    val request: WSRequest = wsUrl(s"/$nino/summary/current-income?journeyId=12345").withHeaders(acceptJsonHeader)

    "return OK and a full valid MobilePayeResponse json" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(request.get())
      response.status shouldBe 200
      response.body shouldBe Json.toJson(fullMobilePayeResponse).toString()
    }

    "return OK and a valid MobilePayeResponse json without employments" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes.filter(_.componentType == PensionIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(request.get())
      response.status shouldBe 200
      response.body shouldBe Json.toJson(fullMobilePayeResponse.copy(employments = None)).toString()
    }

    "return OK and a valid MobilePayeResponse json without pensions" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes.filter(_.componentType == EmploymentIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(request.get())
      response.status shouldBe 200
      response.body shouldBe Json.toJson(fullMobilePayeResponse.copy(pensions = None)).toString()
    }

    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, taxCodeIncomes)
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome.copy(otherNonTaxCodeIncomes = Nil))
      employmentsAreFound(nino.toString(), taiEmployments)
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(request.get())
      response.status shouldBe 200
      response.body shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = None)).toString()
    }

    "return GONE when person is deceased" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person.copy(isDeceased = true))

      val response = await(request.get())
      response.status shouldBe 410

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }

    "return LOCKED when person data is corrupt" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person.copy(hasCorruptData = true))

      val response = await(request.get())
      response.status shouldBe 423

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }
  }
}