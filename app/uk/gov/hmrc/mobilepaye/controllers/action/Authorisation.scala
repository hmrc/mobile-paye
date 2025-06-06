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
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.controllers._
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

case class Authority(nino: Nino)

trait Authorisation extends Results with AuthorisedFunctions {

  val confLevel: Int
  val logger: Logger = Logger(this.getClass)

  lazy val requiresAuth: Boolean = true
  lazy val ninoNotFoundOnAccount = new NinoNotFoundOnAccount
  lazy val failedToMatchNino     = new FailToMatchTaxIdOnAuth
  lazy val lowConfidenceLevel    = new AccountWithLowCL

  def grantAccess(
    requestedNino: Nino
  )(implicit hc:   HeaderCarrier,
    ec:            ExecutionContext
  ): Future[Authority] =
    authorised(Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", requestedNino.value)), "Activated", None))
      .retrieve(nino and confidenceLevel) {
        case Some(foundNino) ~ foundConfidenceLevel =>
          if (foundNino.isEmpty) throw ninoNotFoundOnAccount
          if (!foundNino.equals(requestedNino.nino)) throw failedToMatchNino
          if (confLevel > foundConfidenceLevel.level) throw lowConfidenceLevel
          Future(Authority(requestedNino))
        case None ~ _ =>
          throw ninoNotFoundOnAccount
      }

  def invokeAuthBlock[A](
    request:     Request[A],
    block:       Request[A] => Future[Result],
    taxId:       Option[Nino]
  )(implicit ec: ExecutionContext
  ): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    grantAccess(taxId.getOrElse(Nino("")))
      .flatMap { _ =>
        block(request)
      }
      .recover {
        case _: uk.gov.hmrc.http.UpstreamErrorResponse =>
          logger.info("Unauthorized! Failed to grant access since 4xx response!")
          Unauthorized(Json.toJson[ErrorResponse](ErrorUnauthorizedUpstream))

        case _: NinoNotFoundOnAccount =>
          logger.info("Unauthorized! NINO not found on account!")
          Unauthorized(Json.toJson[ErrorResponse](ErrorUnauthorizedNoNino))

        case _: FailToMatchTaxIdOnAuth =>
          logger.info("Forbidden! Failure to match URL NINO against Auth NINO")
          Forbidden(Json.toJson[ErrorResponse](ForbiddenAccess))

        case _: AccountWithLowCL =>
          logger.info("Unauthorized! Account with low CL!")
          Unauthorized(Json.toJson[ErrorResponse](ErrorUnauthorizedLowCL))
      }
  }
}

trait AccessControl extends HeaderValidator with Authorisation {
  outer =>
  def parser: BodyParser[AnyContent]

  def validateAcceptWithAuth(
    rules:       Option[String] => Boolean,
    taxId:       Option[Nino]
  )(implicit ec: ExecutionContext
  ): ActionBuilder[Request, AnyContent] =
    new ActionBuilder[Request, AnyContent] {

      override def parser:                     BodyParser[AnyContent] = outer.parser
      override protected def executionContext: ExecutionContext       = outer.executionContext

      def invokeBlock[A](
        request: Request[A],
        block:   Request[A] => Future[Result]
      ): Future[Result] =
        if (rules(request.headers.get("Accept"))) {
          if (requiresAuth) invokeAuthBlock(request, block, taxId)
          else block(request)
        } else
          Future.successful(
            Status(ErrorAcceptHeaderInvalid.httpStatusCode)(Json.toJson[ErrorResponse](ErrorAcceptHeaderInvalid))
          )
    }

}
