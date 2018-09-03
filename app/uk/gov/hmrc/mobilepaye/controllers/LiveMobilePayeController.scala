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

import javax.inject.Singleton
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future

trait MobilePayeController extends BaseController with HeaderValidator with ErrorHandling {
  def getPayeData(reqNino: Nino, journeyId: Option[String] = None): Action[AnyContent]
}

@Singleton()
class LiveMobilePayeController extends MobilePayeController {

  override val app: String = "Live-Paye-Controller"

  override def getPayeData(reqNino: Nino, journeyId: Option[String] = None): Action[AnyContent] = Action.async {
    implicit request =>
      errorWrapper {
        Future.successful(Ok("Hello world"))
      }
  }

}