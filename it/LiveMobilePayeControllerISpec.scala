import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.mobilepaye.domain.{MobilePayeResponse, OtherIncome, PayeIncome}
import utils.BaseISpec
import stubs.AuthStub._
import stubs.TaiStub._
import uk.gov.hmrc.mobilepaye.domain.tai._
import play.api.mvc.Results._

class LiveMobilePayeControllerISpec extends BaseISpec {

  val taxCodeIncome: TaxCodeIncome = TaxCodeIncome(EmploymentIncome, Some(3), 1000, "S1150L")
  val otherNonTaxCodeIncome: OtherNonTaxCodeIncome = OtherNonTaxCodeIncome(OtherIncomeEarned, BigDecimal(100.0))
  val nonTaxCodeIncome: NonTaxCodeIncome = NonTaxCodeIncome(None, Seq(otherNonTaxCodeIncome))

  val taiEmployment: Employment = Employment(Some("payrollNum"), 2)

  val taxAccountSummary: TaxAccountSummary = TaxAccountSummary(BigDecimal(200.0), BigDecimal(123.12))

  val person: Person = Person(nino, "Carrot", "Smith", None)

  val payeIncome: PayeIncome = PayeIncome("Sainsburys", None, "S1150L", 1000, "/some/link")

  val employments: Seq[PayeIncome] = Seq(payeIncome, payeIncome.copy(name = "Tesco", amount = 500))
  val pensions: Seq[PayeIncome] = Seq(payeIncome.copy(name = "Prestige Pensions"))
  val otherIncomes: Seq[OtherIncome] = Seq(OtherIncome("SomeOtherIncome", 250))

  val fullResponse = MobilePayeResponse(
    employments = Some(employments),
    pensions = Some(pensions),
    otherIncomes = Some(otherIncomes),
    taxFreeAmount = 10000,
    estimatedTaxAmount = 250
  )

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
      response.body shouldBe Json.toJson(fullResponse)
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
      response.body shouldBe Json.toJson(fullResponse.copy(employments = None))
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
      response.body shouldBe Json.toJson(fullResponse.copy(pensions = None))
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
      response.body shouldBe Json.toJson(fullResponse.copy(otherIncomes = None))
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