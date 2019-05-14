/*
 * Copyright 2019 HM Revenue & Customs
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

import com.google.inject._
import com.google.inject.name.Named
import play.api.libs.json.Json
import play.api.libs.json.Json.obj
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.mobilepaye.config.MobilePayeControllerConfig
import uk.gov.hmrc.mobilepaye.controllers.action.AccessControl
import uk.gov.hmrc.mobilepaye.domain.{MobilePayeResponse, OtherIncome, PayeIncome, Shuttering}
import uk.gov.hmrc.mobilepaye.services.MobilePayeService
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.play.audit.AuditExtensions.auditHeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController
import uk.gov.hmrc.service.Auditor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MobilePayeController extends BackendBaseController with HeaderValidator with ErrorHandling {
  def getPayeSummary(nino: Nino, journeyId: String, taxYear: Int): Action[AnyContent]
}

@Singleton
class LiveMobilePayeController @Inject()(
  override val authConnector:                                   AuthConnector,
  @Named("controllers.confidenceLevel") override val confLevel: Int,
  mobilePayeService:                                            MobilePayeService,
  val controllerComponents:                                     ControllerComponents,
  val auditConnector:                                           AuditConnector,
  @Named("appName") override val appName:                       String,
  config:                                                       MobilePayeControllerConfig
)(
  implicit val executionContext: ExecutionContext
) extends MobilePayeController
    with AccessControl
    with Auditor
    with ControllerChecks {

  override def shuttering: Shuttering             = config.shuttering
  override def parser:     BodyParser[AnyContent] = controllerComponents.parsers.anyContent
  override val app:        String                 = "Live-Paye-Controller"

  override def getPayeSummary(nino: Nino, journeyId: String, taxYear: Int): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, Option(nino)).async { implicit request =>
      implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None).withExtraHeaders(HeaderNames.xSessionId -> journeyId)
      withShuttering(shuttering) {
        errorWrapper {
          mobilePayeService.getPerson(nino).flatMap { person =>
            if (person.isDeceased) {
              Future.successful(Gone)
            } else {
              mobilePayeService.getMobilePayeResponse(nino, taxYear).map { mpr =>
                sendAuditEvent(
                  nino,
                  mpr,
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

  private def sendAuditEvent(nino: Nino, response: MobilePayeResponse, path: String, journeyId: String)(implicit hc: HeaderCarrier): Unit = {
    def removeLinks(data: Option[Seq[PayeIncome]]): Option[Seq[PayeIncome]] = data match {
      case Some(d) if d.nonEmpty => Some(d.map(_.copy(link = None)))
      case None                  => None
    }

    val emps: Option[Seq[PayeIncome]] = removeLinks(response.employments)
    val pens: Option[Seq[PayeIncome]] = removeLinks(response.pensions)
    val otIncs: Option[Seq[OtherIncome]] = response.otherIncomes match {
      case Some(data) if data.nonEmpty => Some(data.map(_.copy(link = None)))
      case None                        => None
    }

    val auditPayload = response.copy(
      employments               = emps,
      pensions                  = pens,
      otherIncomes              = otIncs,
      taxFreeAmountLink         = None,
      estimatedTaxAmountLink    = None,
      understandYourTaxCodeLink = None,
      addMissingEmployerLink    = None,
      addMissingPensionLink     = None,
      addMissingIncomeLink      = None
    )

    auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        appName,
        "viewPayeSummary",
        tags = hc.toAuditTags("view-paye-summary", path),
        detail = obj(
          "nino"      -> nino.value,
          "journeyId" -> journeyId,
          "data"      -> auditPayload
        )))
    ()
  }
}
