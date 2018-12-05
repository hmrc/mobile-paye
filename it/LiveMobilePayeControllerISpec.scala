import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import play.api.mvc.Results._
import stubs.AuthStub._
import stubs.TaiStub._
import uk.gov.hmrc.mobilepaye.domain.tai._
import utils.BaseISpec

class LiveMobilePayeControllerISpec extends BaseISpec {

  s"GET /$nino/summary/current-income" should {
    val request: WSRequest = wsUrl(s"/$nino/summary/current-income").withHeaders(acceptJsonHeader)

    "return OK and a full valid MobilePayeResponse json" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, Seq(taxCodeIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), Seq(taiEmployment))
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(request.get())
      response.status shouldBe Ok
      response.body shouldBe Json.toJson(fullMobilePayeResponse)
    }

    "return OK and a valid MobilePayeResponse json without employments" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, Seq(taxCodeIncome.copy(componentType = PensionIncome)))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), Seq.empty[Employment])
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(request.get())
      response.status shouldBe Ok
      response.body shouldBe Json.toJson(fullMobilePayeResponse.copy(employments = None))
    }

    "return OK and a valid MobilePayeResponse json without pensions" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, Seq(taxCodeIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome)
      employmentsAreFound(nino.toString(), Seq(taiEmployment))
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(request.get())
      response.status shouldBe Ok
      response.body shouldBe Json.toJson(fullMobilePayeResponse.copy(pensions = None))
    }

    "return OK and a valid MobilePayeResponse json without otherIncomes" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person)
      taxCodeIncomesAreFound(nino.toString, Seq(taxCodeIncome))
      nonTaxCodeIncomeIsFound(nino.toString, nonTaxCodeIncome.copy(None, Seq.empty[OtherNonTaxCodeIncome]))
      employmentsAreFound(nino.toString(), Seq(taiEmployment))
      taxAccountSummaryIsFound(nino.toString, taxAccountSummary)

      val response = await(request.get())
      response.status shouldBe Ok
      response.body shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = None))
    }

    "return GONE when person is deceased" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person.copy(isDeceased = true))

      val response = await(request.get())
      response.status shouldBe Gone

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }

    "return LOCKED when person data is corrupt" in {
      grantAccess(nino.toString)
      personalDetailsAreFound(nino.toString, person.copy(hasCorruptData = true))

      val response = await(request.get())
      response.status shouldBe Locked

      taxCodeIncomeNotCalled(nino.toString)
      nonTaxCodeIncomeNotCalled(nino.toString)
      employmentsNotCalled(nino.toString)
      taxAccountSummaryNotCalled(nino.toString)
    }
  }
}