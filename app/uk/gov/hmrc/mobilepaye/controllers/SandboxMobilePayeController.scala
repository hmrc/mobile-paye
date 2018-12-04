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
import play.api.libs.json.Json.toJson
import play.api.mvc._
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain._

import scala.concurrent.Future

@Singleton
class SandboxMobilePayeController @Inject()() extends MobilePayeController with FileResource {

  override val app: String = "Sandbox-Paye-Controller"

  override def getPayeSummary(nino: Nino, journeyId: Option[String] = None): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
        Future successful (request.headers.get("SANDBOX-CONTROL") match {
          case Some("ERROR-401") => Unauthorized
          case Some("ERROR-403") => Forbidden
          case Some("ERROR-500") => InternalServerError
          case Some("NOT-FOUND") => NotFound
          case Some("SINGLE-EMPLOYMENT") =>
            val resource: String = findResource(s"/resources/mobilepayesummary/singleemployment.json")
              .getOrElse(throw new IllegalArgumentException("Resource not found!"))
            Ok(toJson(Json.parse(resource).as[MobilePayeResponse]))
          case Some("SINGLE-PENSION") =>
            val resource: String = findResource(s"/resources/mobilepayesummary/singlepension.json")
              .getOrElse(throw new IllegalArgumentException("Resource not found!"))
            Ok(toJson(Json.parse(resource).as[MobilePayeResponse]))
          case _ =>
            val resource: String = findResource(s"/resources/mobilepayesummary/default.json")
              .getOrElse(throw new IllegalArgumentException("Resource not found!"))
            Ok(toJson(Json.parse(resource).as[MobilePayeResponse]))
        })
    }
}