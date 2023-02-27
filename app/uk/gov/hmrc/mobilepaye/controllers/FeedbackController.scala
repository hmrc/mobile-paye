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

import com.google.inject.Inject
import com.google.inject.name.Named
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, BodyParser, ControllerComponents}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.mobilepaye.controllers.action.AccessControlNoNino
import uk.gov.hmrc.mobilepaye.domain.Feedback
import uk.gov.hmrc.mobilepaye.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.mobilepaye.services.MobilePayeService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter.fromRequest

import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class FeedbackController @Inject()(override val authConnector: AuthConnector,
                                   mobilePayeService: MobilePayeService,
                                   val controllerComponents:
                                   ControllerComponents,
                                   @Named("controllers.confidenceLevel") override val confLevel: Int)(implicit val executionContext: ExecutionContext)
    extends AccessControlNoNino
      with BackendBaseController with ErrorHandling {

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

   val app: String = "Feedback Controller"
  override val logger: Logger = Logger(this.getClass)

   def postFeedback(journeyId: JourneyId): Action[JsValue] =
    validateAcceptWithAuth(acceptHeaderValidationRules).async(parse.json) { implicit request =>
      implicit val hc: HeaderCarrier =
        fromRequest(request).withExtraHeaders(HeaderNames.xSessionId -> journeyId.value)
      errorWrapper {
        withJsonBody[Feedback] { feedbackModel =>
          mobilePayeService.postFeedback(feedbackModel)(hc).map { _ =>
            logger.info(s"[FeedbackController][postFeedback] feedbackModel posted ${request.body}")
            NoContent
          }
        }
      }
    }
}
