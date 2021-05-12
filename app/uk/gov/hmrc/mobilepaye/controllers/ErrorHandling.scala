/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.Json.toJson
import play.api.mvc.Result
import play.api.{Logger, mvc}
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case object ErrorNinoInvalid extends ErrorResponse(400, "NINO_INVALID", "The provided NINO is invalid")

case object ErrorUnauthorizedNoNino extends ErrorResponse(401, "UNAUTHORIZED", "NINO does not exist on account")

case object ErrorUnauthorizedUpstream
    extends ErrorResponse(401, "UNAUTHORIZED", "Upstream service such as auth returned 401")

case object ErrorBadRequest extends ErrorResponse(400, "BAD_REQUEST", "Invalid POST request")

case object LockedUserRequest
    extends ErrorResponse(423, "LOCKED", "Locked! User is locked due to manual correspondence indicator flag being set")

case object MandatoryResponse extends ErrorResponse(500, "MANDATORY", "Mandatory data not found")

case object ForbiddenAccess extends ErrorResponse(403, "UNAUTHORIZED", "Access denied!")

class BadRequestException(message: String) extends HttpException(message, 400)

class GrantAccessException(message: String) extends HttpException(message, 401)

class FailToMatchTaxIdOnAuth extends GrantAccessException("Unauthorised! Failure to match URL NINO against Auth NINO")

class NinoNotFoundOnAccount extends GrantAccessException("Unauthorised! NINO not found on account!")

class AccountWithLowCL extends GrantAccessException("Unauthorised! Account with low CL!")

trait ErrorHandling {
  self: BackendBaseController =>
  val app: String
  val logger: Logger = Logger(this.getClass)

  def log(message: String): Unit = logger.info(s"$app $message")

  def errorWrapper(func: => Future[mvc.Result])(implicit hc: HeaderCarrier): Future[Result] =
    func.recover {
      case _: NotFoundException =>
        log("Resource not found!")
        Status(ErrorNotFound.httpStatusCode)(toJson(ErrorNotFound))

      case _: BadRequestException =>
        log("BadRequest!")
        Status(ErrorBadRequest.httpStatusCode)(toJson(ErrorBadRequest))

      case ex: Upstream4xxResponse if ex.upstreamResponseCode == 401 =>
        log("Upstream service returned 401")
        Status(ErrorUnauthorizedUpstream.httpStatusCode)(toJson(ErrorUnauthorizedUpstream))

      case ex: Upstream4xxResponse if ex.upstreamResponseCode == 423 =>
        log("Locked! User is locked due to manual correspondence indicator flag being set")
        Status(LockedUserRequest.httpStatusCode)(toJson(LockedUserRequest))

      case _: AuthorisationException =>
        log("Unauthorised! Failure to authorise account or grant access")
        Unauthorized(toJson(ErrorUnauthorizedUpstream))

      case e: Exception =>
        logger.warn(s"Native Error - $app Internal server error: ${e.getMessage}", e)
        Status(ErrorInternalServerError.httpStatusCode)(toJson(ErrorInternalServerError))
    }

}
