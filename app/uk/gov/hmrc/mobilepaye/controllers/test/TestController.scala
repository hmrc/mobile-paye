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

package uk.gov.hmrc.mobilepaye.controllers.test

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.mobilepaye.domain.admin.FeatureFlagName
import uk.gov.hmrc.mobilepaye.repository.admin.AdminRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestController @Inject() (
  adminRepo:                AdminRepository,
  val controllerComponents: ControllerComponents
)(implicit ec:              ExecutionContext)
    extends BackendBaseController {

  def setFlag(
    flagName: FeatureFlagName,
    enabled:  Boolean
  ): Action[AnyContent] = Action.async {
    adminRepo.setFeatureFlag(flagName, enabled).map {
      case true  => Created
      case false => NotFound
    }
  }
}
