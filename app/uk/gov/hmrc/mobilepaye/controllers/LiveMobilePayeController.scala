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
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.mobilepaye.controllers.action.AccessControl
import uk.gov.hmrc.mobilepaye.services.MobilePayeService
import uk.gov.hmrc.play.HeaderCarrierConverter.fromHeadersAndSession
import uk.gov.hmrc.play.bootstrap.controller.BackendBaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait MobilePayeController extends BackendBaseController with HeaderValidator with ErrorHandling {
  def getPayeSummary(nino: Nino, journeyId: String): Action[AnyContent]
}

@Singleton
class LiveMobilePayeController @Inject()(
  override val authConnector:                                   AuthConnector,
  @Named("controllers.confidenceLevel") override val confLevel: Int,
  mobilePayeService:                                            MobilePayeService,
  val controllerComponents:                                     ControllerComponents
)(
  implicit val executionContext: ExecutionContext
) extends MobilePayeController
    with AccessControl {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent
  override val app:    String                 = "Live-Paye-Controller"

  override def getPayeSummary(nino: Nino, journeyId: String): Action[AnyContent] =
    validateAcceptWithAuth(acceptHeaderValidationRules, Option(nino)).async { implicit request =>
      implicit val hc: HeaderCarrier = fromHeadersAndSession(request.headers, None).withExtraHeaders(HeaderNames.xSessionId -> journeyId)
      errorWrapper {
        mobilePayeService.getPerson(nino).flatMap { person =>
          (person.isDeceased, person.hasCorruptData) match {
            case (true, _) => Future.successful(Gone)
            case (_, true) => Future.successful(Locked)
            case _ =>
              mobilePayeService.getMobilePayeResponse(nino).map { mpr =>
                Ok(Json.toJson(mpr))
              }
          }
        }

      }
    }
}
