/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.MobilePayeResponse
import uk.gov.hmrc.mobilepaye.services.MobilePayeService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MobilePayeController extends BaseController with HeaderValidator with ErrorHandling {
  def getPayeSummary(nino: Nino, journeyId: Option[String] = None): Action[AnyContent]
}

@Singleton
class LiveMobilePayeController @Inject()(mobilePayeService: MobilePayeService) extends MobilePayeController {

  override val app: String = "Live-Paye-Controller"

  override def getPayeSummary(nino: Nino, journeyId: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request =>
      errorWrapper {
        mobilePayeService.getPerson(nino).flatMap {
          person =>
            (person.isDeceased, person.hasCorruptData) match {
              case (true, _) => Future.successful(Gone)
              case (_, true) => Future.successful(Locked)
              case _ => mobilePayeService.getMobilePayeResponse(nino).map {
                case MobilePayeResponse(_, None, None, None, _, _, _, _, _, _, _, _) => NotFound
                case mobilePayeResponse => Ok(Json.toJson(mobilePayeResponse))
              }
            }
        }

      }
  }

}