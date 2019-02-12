import play.api.libs.json.Json
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.mobilepaye.domain.Shuttering
import utils.BaseISpec

class SandboxMobilePayeControllerISpec extends BaseISpec {

  private val mobileHeader = "X-MOBILE-USER-ID" -> "208606423740"
  override def shuttered: Boolean = false

  s"GET sandbox/nino/$nino/tax-year/$currentTaxYear/summary" should {
    val request: WSRequest = wsUrl(s"/nino/$nino/tax-year/$currentTaxYear/summary?journeyId=12345").addHttpHeaders(acceptJsonHeader)

    "return OK and default paye data with no SANDBOX-CONTROL" in {
      val response = await(request.addHttpHeaders(mobileHeader).get())
      response.status shouldBe 200
      (response.json \\ "employments") should not be empty
      (response.json \\ "pensions") should not be empty
      (response.json \\ "otherIncomes") should not be empty
      (response.json \ "taxFreeAmount").as[Int] shouldBe 11850
      (response.json \ "estimatedTaxAmount").as[Int] shouldBe 618
    }

    "return OK and a single employment with no pension or otherIncome data when SANDBOX-CONTROL is SINGLE-EMPLOYMENT" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "SINGLE-EMPLOYMENT").get())
      response.status shouldBe 200
      (response.json \\ "employments") should not be empty
      (response.json \ "employments" \ 0 \ "name").as[String] shouldBe "SAINSBURY'S PLC"
      (response.json \\ "pensions") shouldBe empty
      (response.json \\ "otherIncomes") shouldBe empty
      (response.json \ "taxFreeAmount").as[Int] shouldBe 11850
      (response.json \ "estimatedTaxAmount").as[Int] shouldBe 618
    }

    "return OK and a single pension with no employment or otherIncome data when SANDBOX-CONTROL is SINGLE-PENSION" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "SINGLE-PENSION").get())
      response.status shouldBe 200
      (response.json \\ "employments") shouldBe empty
      (response.json \\ "pensions") should not be empty
      (response.json \ "pensions" \ 0 \ "name").as[String] shouldBe "HIGHWIRE RETURNS LTD"
      (response.json \\ "otherIncomes") shouldBe empty
      (response.json \ "taxFreeAmount").as[Int] shouldBe 11850
      (response.json \ "estimatedTaxAmount").as[Int] shouldBe 618
    }

    "return OK and only TaxYear with 'missing' links when SANDBOX-CONTROL is NO-TAX-YEAR-INCOME" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "NO-TAX-YEAR-INCOME").get())
      response.status shouldBe 200
      (response.json \\ "employments") shouldBe empty
      (response.json \\ "pensions") shouldBe empty
      (response.json \\ "otherIncomes") shouldBe empty
      (response.json \\ "understandYourTaxCodeLink") shouldBe empty
    }

    "return OK and only TaxYear with 'missing' links when SANDBOX-CONTROL is PREVIOUS-INCOME-ONLY" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "PREVIOUS-INCOME-ONLY").get())
      response.status shouldBe 200
      (response.json \\ "employments") shouldBe empty
      (response.json \\ "pensions") shouldBe empty
      (response.json \\ "otherIncomes") shouldBe empty
      (response.json \ "understandYourTaxCodeLink").as[String] shouldBe "/"
    }

    "return OK and only TaxYear with 'missing' links when SANDBOX-CONTROL is OTHER-INCOME-ONLY" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "OTHER-INCOME-ONLY").get())
      response.status shouldBe 200
      (response.json \\ "employments") shouldBe empty
      (response.json \\ "pensions") shouldBe empty
      (response.json \\ "otherIncomes") should not be empty
      (response.json \ "understandYourTaxCodeLink").as[String] shouldBe "/"
    }

    "return 404 where SANDBOX-CONTROL is NOT-FOUND" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "NOT-FOUND").get())
      response.status shouldBe 404
    }

    "return 410 if the person is deceased where SANDBOX-CONTROL is DECEASED" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "DECEASED").get())
      response.status shouldBe 410
    }

    "return 423 if manual correspondence is required where SANDBOX-CONTROL is MCI" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "MCI").get())
      response.status shouldBe 423
    }

    "return 401 if unauthenticated where SANDBOX-CONTROL is ERROR-401" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-401").get())
      response.status shouldBe 401
    }

    "return 403 if forbidden where SANDBOX-CONTROL is ERROR-403" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-403").get())
      response.status shouldBe 403
    }

    "return 500 if there is an error where SANDBOX-CONTROL is ERROR-500" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "ERROR-500").get())
      response.status shouldBe 500
    }

    "return 521 if there is an error where SANDBOX-CONTROL is SHUTTERD" in {
      val response = await(request.addHttpHeaders(mobileHeader, "SANDBOX-CONTROL" -> "SHUTTERED").get())
      response.status shouldBe 521

      val shuttering = Json.parse(response.body).as[Shuttering]
      shuttering.shuttered shouldBe true
      shuttering.title shouldBe "Shuttered"
      shuttering.message shouldBe "PAYE is currently shuttered"

    }
  }
}
