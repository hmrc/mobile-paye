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

package uk.gov.hmrc.mobilepaye.controllers.action

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.api.controllers._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.controllers._
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait AuthorisationNoNino extends Results with AuthorisedFunctions {

  val confLevel: Int
  val logger: Logger = Logger(this.getClass)

  lazy val requiresAuth       = true
  lazy val lowConfidenceLevel = new AccountWithLowCL

  def grantAccess()(implicit hc: HeaderCarrier): Future[Boolean] =
    authorised(CredentialStrength("strong") and ConfidenceLevel.L200)
      .retrieve(confidenceLevel) { foundConfidenceLevel =>
        if (confLevel > foundConfidenceLevel.level) throw lowConfidenceLevel
        else Future successful true
      }

  def invokeAuthBlock[A](
                          request: Request[A],
                          block:   Request[A] => Future[Result]
                        ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    grantAccess()
      .flatMap { _ =>
        block(request)
      }
      .recover {
        case _: uk.gov.hmrc.http.Upstream4xxResponse =>
          logger.info("Unauthorized! Failed to grant access since 4xx response!")
          Unauthorized(Json.toJson(ErrorUnauthorizedUpstream.asInstanceOf[ErrorResponse]))

        case _: AccountWithLowCL =>
          logger.info("Unauthorized! Account with low CL!")
          Unauthorized(Json.toJson(ErrorUnauthorizedLowCL.asInstanceOf[ErrorResponse]))
      }
  }
}

trait AccessControlNoNino extends HeaderValidator with AuthorisationNoNino {
  outer =>
  def parser: BodyParser[AnyContent]

  def validateAcceptWithAuth(rules: Option[String] => Boolean): ActionBuilder[Request, AnyContent] =
    new ActionBuilder[Request, AnyContent] {

      override def parser:                     BodyParser[AnyContent] = outer.parser
      override protected def executionContext: ExecutionContext       = outer.executionContext

      def invokeBlock[A](
                          request: Request[A],
                          block:   Request[A] => Future[Result]
                        ): Future[Result] =
        if (rules(request.headers.get("Accept"))) {
          if (requiresAuth) invokeAuthBlock(request, block)
          else block(request)
        } else
          Future.successful(
            Status(ErrorAcceptHeaderInvalid.httpStatusCode)(
              Json.toJson(ErrorAcceptHeaderInvalid.asInstanceOf[ErrorResponse])
            )
          )
    }
}

