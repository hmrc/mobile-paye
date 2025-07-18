/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.controllers

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.libs.json.JsValue
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L200, L50}
import uk.gov.hmrc.auth.core.syntax.retrieved.*
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.*
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.mobilepaye.domain.{IncomeTaxYear, MobilePayePreviousYearSummaryResponse, MobilePayeSummaryResponse, Shuttering}
import uk.gov.hmrc.mobilepaye.services.{IncomeTaxHistoryService, MobilePayeService, PreviousYearSummaryService}
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import eu.timepit.refined.auto.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.mobilepaye.domain.citizendetails.Person
import uk.gov.hmrc.mobilepaye.domain.types.JourneyId
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import org.scalamock.matchers.MatcherBase
import uk.gov.hmrc.mobilepaye.connectors.ShutteringConnector
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, OnlinePaymentIntegration}
import uk.gov.hmrc.mobilepaye.domain.audit.MobilePayeSummaryResponseAudit
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import scala.concurrent.{ExecutionContext, Future}

class LiveMobilePayeControllerSpec extends BaseSpec {

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders(acceptHeader)
  val grantAccessWithCL200 = Some(nino.toString()) and L200
  val mockMobilePayeService: MobilePayeService = mock[MobilePayeService]
  val mockIncomeTaxHistoryService: IncomeTaxHistoryService = mock[IncomeTaxHistoryService]
  val mockPreviousYearSummaryService: PreviousYearSummaryService = mock[PreviousYearSummaryService]
  override implicit val mockAuditConnector: AuditConnector = mock[AuditConnector]

  type GrantAccess = Option[String] ~ ConfidenceLevel

  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val mockShutteringConnector: ShutteringConnector = mock[ShutteringConnector]

  private val shuttered = Shuttering(shuttered = true, Some("Shuttered"), Some("PAYE is currently not available"))
  private val notShuttered = Shuttering.shutteringDisabled
  val jId1: JourneyId = JourneyId.from("9bcb9c5a-0cfd-49e3-a935-58a28c386a42").toOption.get

  def mockShutteringResponse(
    response: Shuttering
  )(implicit shutteringConnector: ShutteringConnector) = {
    (shutteringConnector
      .getShutteringStatus(_: JourneyId, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future successful response)
  }

  def mockAuthorisationGrantAccess(
    response: GrantAccess
  )(implicit authConnector: AuthConnector) = {
    (authConnector
      .authorise(_: Predicate, _: Retrieval[GrantAccess])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future successful response)
  }

  def dataEventWith(
    auditSource: String,
    auditType: String,
    transactionName: String,
    detail: JsValue
  ): MatcherBase =
    argThat { (dataEvent: ExtendedDataEvent) =>
      dataEvent.auditSource.equals(auditSource) &&
      dataEvent.auditType.equals(auditType) &&
      dataEvent.tags("transactionName").equals(transactionName) &&
      dataEvent.tags.get("path").isDefined &&
      dataEvent.tags.get("clientIP").isDefined &&
      dataEvent.tags.get("clientPort").isDefined &&
      dataEvent.tags.get("X-Request-ID").isDefined &&
      dataEvent.tags.get("X-Session-ID").isDefined &&
      dataEvent.tags.get("Unexpected").isEmpty &&
      dataEvent.detail.equals(detail)
    }

  def mockAudit(
    nino: Nino,
    expectedDetails: MobilePayeSummaryResponseAudit,
    journeyId: JourneyId
  )(implicit auditConnector: AuditConnector): Unit = {
    (auditConnector
      .sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returning(Future successful Success)
    ()
  }

  def controller =
    new LiveMobilePayeController(
      mockAuthConnector,
      200,
      mockMobilePayeService,
      stubControllerComponents(),
      mockAuditConnector,
      "mobile-paye",
      mockShutteringConnector,
      mockIncomeTaxHistoryService,
      mockPreviousYearSummaryService
    )

  def mockGetMobilePayeResponse(f: Future[MobilePayeSummaryResponse]) =
    (mockMobilePayeService
      .getMobilePayeSummaryResponse(_: Nino, _: Int, _: JourneyId)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returning(f)

  def mockGetPerson(f: Future[Person]) =
    (mockMobilePayeService.getPerson(_: Nino)(_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *).returning(f)

  def mockGetIncomeTaxHistoryYearsList(f: Future[List[IncomeTaxYear]]) =
    (mockIncomeTaxHistoryService
      .getIncomeTaxHistoryYearsList(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returning(f)

  def mockGetMobilePayePreviousYearResponse(f: Future[MobilePayePreviousYearSummaryResponse]) =
    (mockPreviousYearSummaryService
      .getMobilePayePreviousYearSummaryResponse(_: Nino, _: Int)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(f)

  def mockGetCurrentTaxCode(f: Future[Option[String]]) =
    (mockMobilePayeService
      .getCurrentTaxCode(_: Nino)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returning(f)

  "getPayeSummary" should {

    "return 200 and full paye summary data for valid authorised nino" in {

      mockAuthorisationGrantAccess(grantAccessWithCL200)(mockAuthConnector)
      mockShutteringResponse(notShuttered)(mockShutteringConnector)
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(
        Future.successful(fullMobilePayeResponse.copy(simpleAssessment = Some(fullMobileSimpleAssessmentResponse)))
      )

      mockAudit(nino, fullMobilePayeAudit.copy(simpleAssessment = Some(fullMobileSimpleAssessmentResponse)), jId1)

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(
        fullMobilePayeResponse.copy(simpleAssessment = Some(fullMobileSimpleAssessmentResponse))
      )
    }

    "return 200 and paye summary data with no employment data for valid authorised nino" in {
      mockShutteringResponse(notShuttered)
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.successful(fullMobilePayeResponse.copy(employments = None)))
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockAudit(nino, fullMobilePayeAudit.copy(employments = None), jId1)

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(fullMobilePayeResponse.copy(employments = None))
    }

    "return 200 and paye summary data with no pensions data for valid authorised nino" in {
      mockShutteringResponse(notShuttered)
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.successful(fullMobilePayeResponse.copy(pensions = None)))
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockAudit(nino, fullMobilePayeAudit.copy(pensions = None), jId1)

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(fullMobilePayeResponse.copy(pensions = None))
    }

    "return 200 and paye summary data with no other income data for valid authorised nino" in {
      mockShutteringResponse(notShuttered)
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.successful(fullMobilePayeResponse.copy(otherIncomes = None)))
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockAudit(nino, fullMobilePayeAudit.copy(otherIncomes = None), jId1)

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = None))
    }

    "return 423 for a valid nino and authorised user but LOCKED returned" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPerson(Future.failed(UpstreamErrorResponse("locked", 423, 423)))

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 423
    }

    "return 410 for a valid nino and authorised user but deceased user" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPerson(Future.successful(person.copy(deceased = true)))

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 410
    }

    "return 500 when MobilePayeService throws an InternalServerException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.failed(new InternalServerException("Internal Server Error")))

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 500
    }

    "return 401 for valid nino and user but low CL" in {
      mockAuthorisationGrantAccess(Some(nino.toString()) and L50)

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 401
    }

    "return 406 for missing accept header" in {
      val result =
        controller.getPayeSummary(nino, jId1, currentTaxYear)(FakeRequest("GET", "/"))

      status(result) shouldBe 406
    }

    "return 403 for valid nino for authorised user but for a different nino" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)

      val result = controller.getPayeSummary(Nino("CS100700A"), jId1, currentTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 403
    }

    "return 404 when handling NotFoundException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPerson(Future.failed(new NotFoundException("Not Found Exception")))

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 404
    }

    "return 400 when handling BadRequestException" in {
      import scala.util.{Success, Failure}
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(notShuttered)
      mockGetPerson(Future.failed(new BadRequestException("Bad Request Exception")))
      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)
      status(result) shouldBe 400
    }

    "return 401 when handling 401 Upstream4xxResponse" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPerson(Future.failed(UpstreamErrorResponse("Upstream Exception", 401, 401)))

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 401
    }

    "return 401 when handling AuthorisationException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetPerson(Future.failed(new MissingBearerToken))

      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 401
    }

    "return 521 when shuttered" in {
      mockShutteringResponse(shuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      val result = controller.getPayeSummary(nino, jId1, currentTaxYear)(fakeRequest)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message").as[String]    shouldBe "PAYE is currently not available"
    }
  }

  "getPreviousYearPayeSummary" should {
    "return 200 and previous year's paye summary data for valid authorised nino" in {
      mockShutteringResponse(notShuttered)
      mockGetMobilePayePreviousYearResponse(
        Future.successful(fullMobilePayePreviousYearResponse())
      )
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockAudit(nino, fullMobilePayePYAudit(), jId1)

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(fullMobilePayePreviousYearResponse())
    }

    "return 200 and paye summary data with no employment data for valid authorised nino" in {
      mockShutteringResponse(notShuttered)
      mockGetMobilePayePreviousYearResponse(
        Future.successful(fullMobilePayePreviousYearResponse().copy(employments = None))
      )
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockAudit(nino, fullMobilePayePYAudit().copy(employments = None), jId1)

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(fullMobilePayePreviousYearResponse().copy(employments = None))
    }

    "return 200 and paye summary data with no pensions data for valid authorised nino" in {
      mockShutteringResponse(notShuttered)
      mockGetMobilePayePreviousYearResponse(
        Future.successful(fullMobilePayePreviousYearResponse().copy(pensions = None))
      )
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockAudit(nino, fullMobilePayePYAudit().copy(pensions = None), jId1)

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(fullMobilePayePreviousYearResponse().copy(pensions = None))
    }

    "return 200 and paye summary data with no other income data for valid authorised nino" in {
      mockShutteringResponse(notShuttered)
      mockGetMobilePayePreviousYearResponse(
        Future.successful(fullMobilePayePreviousYearResponse().copy(otherIncomes = None))
      )
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockAudit(nino, fullMobilePayePYAudit().copy(otherIncomes = None), jId1)

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(fullMobilePayePreviousYearResponse().copy(otherIncomes = None))
    }

    "return 500 when MobilePayeService throws an InternalServerException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetMobilePayePreviousYearResponse(Future.failed(new InternalServerException("Internal Server Error")))

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 500
    }

    "return 401 for valid nino and user but low CL" in {
      mockAuthorisationGrantAccess(Some(nino.toString()) and L50)

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 401
    }

    "return 406 for missing accept header" in {
      val result =
        controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
          FakeRequest("GET", "/")
        )

      status(result) shouldBe 406
    }

    "return 403 for valid nino for authorised user but for a different nino" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)

      val result = controller.getPreviousYearPayeSummary(Nino("CS100700A"), jId1, previousTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 403
    }

    "return 404 when handling NotFoundException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetMobilePayePreviousYearResponse(Future.failed(new NotFoundException("Not Found Exception")))

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 404
    }

    "return 400 when handling BadRequestException" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockShutteringResponse(notShuttered)
      mockGetMobilePayePreviousYearResponse(Future.failed(new BadRequestException("Bad Request Exception")))

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 400
    }

    "return 401 when handling 401 Upstream4xxResponse" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetMobilePayePreviousYearResponse(Future.failed(UpstreamErrorResponse("Upstream Exception", 401, 401)))

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 401
    }

    "return 401 when handling AuthorisationException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetMobilePayePreviousYearResponse(Future.failed(new MissingBearerToken))

      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 401
    }

    "return 521 when shuttered" in {
      mockShutteringResponse(shuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      val result = controller.getPreviousYearPayeSummary(nino, jId1, previousTaxYear)(
        fakeRequest
      )

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message").as[String]    shouldBe "PAYE is currently not available"
    }
  }

  "getIncomeTaxHistory" should {
    "return 200 and full income tax history" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetIncomeTaxHistoryYearsList(Future successful fullIncomeTaxHistoryList)

      val result = controller.getTaxIncomeHistory(nino, jId1)(fakeRequest)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.toJson(fullIncomeTaxHistoryList)
    }

    "return 500 when IncomeTaxService throws an InternalServerException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetIncomeTaxHistoryYearsList(Future.failed(new InternalServerException("Internal Server Error")))

      val result = controller.getTaxIncomeHistory(nino, jId1)(fakeRequest)

      status(result) shouldBe 500
    }

    "return 401 for valid nino and user but low CL" in {
      mockAuthorisationGrantAccess(Some(nino.toString()) and L50)

      val result = controller.getTaxIncomeHistory(nino, jId1)(fakeRequest)

      status(result) shouldBe 401
    }

    "return 406 for missing accept header" in {
      val result = controller.getTaxIncomeHistory(nino, jId1)(FakeRequest("GET", "/"))

      status(result) shouldBe 406
    }

    "return 403 for valid nino for authorised user but for a different nino" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)

      val result =
        controller.getTaxIncomeHistory(Nino("CS100700A"), jId1)(fakeRequest)

      status(result) shouldBe 403
    }

    "return 404 when handling NotFoundException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetIncomeTaxHistoryYearsList(Future.failed(new NotFoundException("Not Found Exception")))

      val result = controller.getTaxIncomeHistory(nino, jId1)(fakeRequest)

      status(result) shouldBe 404
    }

    "return 521 when shuttered" in {
      mockShutteringResponse(shuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      val result = controller.getTaxIncomeHistory(nino, jId1)(fakeRequest)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message").as[String]    shouldBe "PAYE is currently not available"
    }
  }

  "getTaxCode" should {
    "return 200 and tax code" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetCurrentTaxCode(Future successful Some(taxCodeIncome.taxCode))

      val result = controller.getTaxCode(nino, jId1)(fakeRequest)

      status(result)        shouldBe 200
      contentAsJson(result) shouldBe Json.obj("taxCode" -> taxCodeIncome.taxCode)
    }

    "return 404 if no current tax code found" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetCurrentTaxCode(Future successful None)

      val result = controller.getTaxCode(nino, jId1)(fakeRequest)

      status(result) shouldBe 404
    }

    "return 500 when IncomeTaxService throws an InternalServerException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetCurrentTaxCode(Future.failed(new InternalServerException("Internal Server Error")))

      val result = controller.getTaxCode(nino, jId1)(fakeRequest)

      status(result) shouldBe 500
    }

    "return 401 for valid nino and user but low CL" in {
      mockAuthorisationGrantAccess(Some(nino.toString()) and L50)

      val result = controller.getTaxCode(nino, jId1)(fakeRequest)

      status(result) shouldBe 401
    }

    "return 406 for missing accept header" in {
      val result = controller.getTaxCode(nino, jId1)(FakeRequest("GET", "/"))

      status(result) shouldBe 406
    }

    "return 403 for valid nino for authorised user but for a different nino" in {
      mockAuthorisationGrantAccess(grantAccessWithCL200)

      val result =
        controller.getTaxCode(Nino("CS100700A"), jId1)(fakeRequest)

      status(result) shouldBe 403
    }

    "return 404 when handling NotFoundException" in {
      mockShutteringResponse(notShuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      mockGetCurrentTaxCode(Future.failed(new NotFoundException("Not Found Exception")))

      val result = controller.getTaxCode(nino, jId1)(fakeRequest)

      status(result) shouldBe 404
    }

    "return 521 when shuttered" in {
      mockShutteringResponse(shuttered)
      mockAuthorisationGrantAccess(grantAccessWithCL200)
      val result = controller.getTaxCode(nino, jId1)(fakeRequest)

      status(result) shouldBe 521
      val jsonBody = contentAsJson(result)
      (jsonBody \ "shuttered").as[Boolean] shouldBe true
      (jsonBody \ "title").as[String]      shouldBe "Shuttered"
      (jsonBody \ "message").as[String]    shouldBe "PAYE is currently not available"
    }
  }
}
