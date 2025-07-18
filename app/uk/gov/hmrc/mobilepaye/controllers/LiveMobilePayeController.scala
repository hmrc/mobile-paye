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

package uk.gov.hmrc.mobilepaye.controllers

import com.google.inject.*
import com.google.inject.name.Named
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.Json.obj
import play.api.mvc.*
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.mobilepaye.connectors.ShutteringConnector
import uk.gov.hmrc.mobilepaye.controllers.action.AccessControl
import uk.gov.hmrc.mobilepaye.domain.audit.MobilePayeSummaryResponseAudit
import uk.gov.hmrc.mobilepaye.domain.types.JourneyId
import uk.gov.hmrc.mobilepaye.services.{IncomeTaxHistoryService, MobilePayeService, PreviousYearSummaryService}
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import scala.concurrent.{ExecutionContext, Future}

trait MobilePayeController extends BackendBaseController with HeaderValidator with ErrorHandling {

  def getPayeSummary(
    nino: Nino,
    journeyId: JourneyId,
    taxYear: Int
  ): Action[AnyContent]

  def getTaxIncomeHistory(
    nino: Nino,
    journeyId: JourneyId
  ): Action[AnyContent]

  def getPreviousYearPayeSummary(
    nino: Nino,
    journeyId: JourneyId,
    taxYear: Int
  ): Action[AnyContent]

  def getTaxCode(
    nino: Nino,
    journeyId: JourneyId
  ): Action[AnyContent]
}

@Singleton
class LiveMobilePayeController @Inject() (
  override val authConnector: AuthConnector,
  @Named("controllers.confidenceLevel") override val confLevel: Int,
  mobilePayeService: MobilePayeService,
  val controllerComponents: ControllerComponents,
  val auditConnector: AuditConnector,
  @Named("appName") val appName: String,
  shutteringConnector: ShutteringConnector,
  incomeTaxHistoryService: IncomeTaxHistoryService,
  previousYearSummaryService: PreviousYearSummaryService
)(implicit val executionContext: ExecutionContext)
    extends MobilePayeController
    with AccessControl
    with ControllerChecks {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent
  override val app: String = "Live-Paye-Controller"
  override val logger: Logger = Logger(this.getClass)

  override def getPayeSummary(
    nino: Nino,
    journeyId: JourneyId,
    taxYear: Int
  ): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, Option(nino)).async { implicit request =>
      implicit val hc: HeaderCarrier =
        fromRequest(request).withExtraHeaders(HeaderNames.xSessionId -> journeyId.value.toString)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          errorWrapper {
            mobilePayeService.getPerson(nino).flatMap { person =>
              if (person.deceased) {
                Future.successful(Gone)
              } else {
                mobilePayeService.getMobilePayeSummaryResponse(nino, taxYear, journeyId).map { mpr =>
                  sendAuditEvent(
                    nino,
                    MobilePayeSummaryResponseAudit.fromResponse(mpr),
                    request.path,
                    journeyId
                  )
                  Ok(Json.toJson(mpr))
                }
              }
            }
          }
        }
      }
    }

  override def getTaxIncomeHistory(
    nino: Nino,
    journeyId: JourneyId
  ): Action[AnyContent] = validateAcceptWithAuth(acceptHeaderValidationRules, Option(nino)).async { implicit request =>
    implicit val hc: HeaderCarrier =
      fromRequest(request).withExtraHeaders(HeaderNames.xSessionId -> journeyId.value.toString)
    shutteringConnector.getShutteringStatus(journeyId, "mobile-paye-income-tax-history").flatMap { shuttered =>
      withShuttering(shuttered) {
        errorWrapper {
          incomeTaxHistoryService.getIncomeTaxHistoryYearsList(nino).map { incomeTaxHistory =>
            Ok(Json.toJson(incomeTaxHistory))
          }
        }

      }
    }
  }

  override def getPreviousYearPayeSummary(
    nino: Nino,
    journeyId: JourneyId,
    taxYear: Int
  ): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, Option(nino)).async { implicit request =>
      implicit val hc: HeaderCarrier =
        fromRequest(request).withExtraHeaders(HeaderNames.xSessionId -> journeyId.value.toString)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          errorWrapper {
            previousYearSummaryService.getMobilePayePreviousYearSummaryResponse(nino, taxYear).map { mobilePayePreviousYearSummaryResponse =>
              sendAuditEvent(
                nino,
                MobilePayeSummaryResponseAudit.fromPYResponse(mobilePayePreviousYearSummaryResponse),
                request.path,
                journeyId
              )
              Ok(Json.toJson(mobilePayePreviousYearSummaryResponse))
            }
          }
        }
      }
    }

  override def getTaxCode(
    nino: Nino,
    journeyId: JourneyId
  ): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, Option(nino)).async { implicit request =>
      implicit val hc: HeaderCarrier =
        fromRequest(request).withExtraHeaders(HeaderNames.xSessionId -> journeyId.value.toString)
      shutteringConnector.getShutteringStatus(journeyId).flatMap { shuttered =>
        withShuttering(shuttered) {
          errorWrapper {
            mobilePayeService.getCurrentTaxCode(nino).map {
              case Some(taxCode) => Ok(Json.obj("taxCode" -> taxCode))
              case _             => NotFound("")
            }
          }
        }
      }
    }

  private def sendAuditEvent(
    nino: Nino,
    response: MobilePayeSummaryResponseAudit,
    path: String,
    journeyId: JourneyId
  )(implicit hc: HeaderCarrier) = auditConnector.sendExtendedEvent(
    ExtendedDataEvent(
      appName,
      "viewPayeSummary",
      tags   = hc.toAuditTags("view-paye-summary", path),
      detail = obj("nino" -> nino.value, "journeyId" -> journeyId.value.toString, "data" -> response)
    )
  )
}
