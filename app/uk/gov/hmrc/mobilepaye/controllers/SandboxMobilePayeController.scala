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

import com.google.inject._
import play.api.Logger
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.api.sandbox.FileResource
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain._
import uk.gov.hmrc.mobilepaye.domain.types.ModelTypes.JourneyId
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SandboxMobilePayeController @Inject() (
  val controllerComponents:      ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends MobilePayeController
    with FileResource {

  override val app:    String = "Sandbox-Paye-Controller"
  override val logger: Logger = Logger(this.getClass)
  private final val WebServerIsDown = new Status(521)

  private val shuttered =
    Json.toJson(Shuttering(shuttered = true, title = Some("Shuttered"), message = Some("PAYE is currently shuttered")))

  override def getPayeSummary(
    nino:      Nino,
    journeyId: JourneyId,
    taxYear:   Int
  ): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      Future.successful(request.headers.get("SANDBOX-CONTROL") match {
        case Some("ERROR-401")            => Unauthorized
        case Some("ERROR-403")            => Forbidden
        case Some("ERROR-500")            => InternalServerError
        case Some("NOT-FOUND")            => NotFound
        case Some("DECEASED")             => Gone
        case Some("MCI")                  => Locked
        case Some("SHUTTERED")            => WebServerIsDown(shuttered)
        case Some("PREVIOUS-INCOME-ONLY") => Ok(readData("previous-income-only.json"))
        case Some("NO-TAX-YEAR-INCOME")   => Ok(readData("no-tax-year-income.json"))
        case Some("SINGLE-EMPLOYMENT")    => Ok(readData("single-employment.json"))
        case Some("SINGLE-PENSION")       => Ok(readData("single-pension.json"))
        case Some("OTHER-INCOME-ONLY")    => Ok(readData("other-income-only.json"))
        case Some("REFUND")               => Ok(readData("repayment-refund.json"))
        case Some("CHEQUE-SENT")          => Ok(readData("cheque-sent.json"))
        case Some("PAYMENT-PAID")         => Ok(readData("payment-paid.json"))
        case Some("PAYMENT-PROCESSING")   => Ok(readData("payment-processing.json"))
        case _                            => Ok(readData("default.json"))
      })
    }

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.anyContent

  override def getTaxIncomeHistory(
    nino:      Nino,
    journeyId: JourneyId
  ): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    Future.successful(
      Ok(
        toJson(
          Json.parse(
            findResource("/resources/mobilepayesummary/income-tax-history-response.json")
              .getOrElse(throw new IllegalArgumentException("Resource not found!"))
              .replace("<TAX_YEAR>", TaxYear.current.currentYear.toString)
              .replace("<TAX_YEAR-1>", TaxYear.current.back(1).currentYear.toString)
              .replace("<TAX_YEAR-2>", TaxYear.current.back(2).currentYear.toString)
              .replace("<TAX_YEAR-3>", TaxYear.current.back(3).currentYear.toString)
              .replace("<TAX_YEAR-4>", TaxYear.current.back(4).currentYear.toString)
              .replace("<TAX_YEAR-5>", TaxYear.current.back(5).currentYear.toString)
          )
        )
      )
    )
  }

  private def readData(resource: String): JsValue =
    toJson(
      Json
        .parse(
          findResource(s"/resources/mobilepayesummary/$resource")
            .getOrElse(throw new IllegalArgumentException("Resource not found!"))
            .replace("<TAX_YEAR>", TaxYear.current.currentYear.toString)
            .replace("<PREVIOUS_TAX_YEAR>", (TaxYear.current.currentYear - 1).toString)
            .replace("<PAYMENT_DATE>", LocalDate.now().plusDays(2).toString)
        )
        .as[MobilePayeSummaryResponse]
    )

  override def getPreviousYearPayeSummary(
    nino:      Nino,
    journeyId: JourneyId,
    taxYear:   Int
  ): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    Future successful NotFound(Json.toJson("No Data found"))
  }
}
