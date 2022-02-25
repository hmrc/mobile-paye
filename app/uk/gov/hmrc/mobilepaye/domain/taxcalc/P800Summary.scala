/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.domain.taxcalc

import java.time.LocalDate

import play.api.libs.json._
import uk.gov.hmrc.mobilepaye.domain.P800Repayment
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status.Overpaid
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus.{Refund, SaUser, UnableToClaim}

import scala.util.{Failure, Success, Try}

case class P800Summary(
  _type:    P800Status,
  status:   Option[RepaymentStatus],
  amount:   Option[BigDecimal],
  datePaid: Option[LocalDate])

object P800Summary {

  def toP800Repayment(
    p800Summary: P800Summary,
    taxYear:     Int
  ): Option[P800Repayment] = {

    val previousTaxYear = taxYear - 1

    def withOverpaidP800(p800Summary: P800Summary): Option[P800Summary] =
      p800Summary._type match {
        case Overpaid => Option(p800Summary)
        case _        => None
      }

    def withAcceptableRepaymentStatus(p800Summary: P800Summary): Option[P800Summary] =
      p800Summary.status match {
        case Some(SaUser)        => None
        case Some(UnableToClaim) => None
        case _                   => Option(p800Summary)
      }

    def notOlderThanSixWeeks(p800Summary: P800Summary): Option[P800Summary] =
      p800Summary.datePaid match {
        case None       => Option(p800Summary)
        case Some(date) => if (date.plusWeeks(6).plusDays(1).isAfter(LocalDate.now())) Option(p800Summary) else None
      }

    def transform(p800Summary: P800Summary): P800Repayment = {
      def withLink: Option[String] =
        p800Summary.status match {
          case Some(Refund) => Option(s"/tax-you-paid/$previousTaxYear-$taxYear/paid-too-much")
          case _            => None
        }

      P800Repayment(p800Summary.amount, p800Summary.status, p800Summary.datePaid, previousTaxYear, withLink)
    }

    for {
      _ <- withOverpaidP800(p800Summary)
      _ <- withAcceptableRepaymentStatus(p800Summary)
      _ <- notOlderThanSixWeeks(p800Summary)
      result = transform(p800Summary)
    } yield result
  }

  implicit val format: Format[P800Summary] = {
    // Historical reasons: https://jira.tools.tax.service.gov.uk/browse/COTCO-632
    // since there is no interest in the hour and the rest, it is trimmed for the LocalDate only

    implicit val format: Format[LocalDate] = new Format[LocalDate] {
      override def writes(o: LocalDate): JsValue = JsString(o.toString)

      override def reads(json: JsValue): JsResult[LocalDate] =
        json.validate[String].flatMap { s =>
          val date = s.take(10)
          Try(LocalDate.parse(date)) match {
            case Success(d)   => JsSuccess(d)
            case Failure(err) => JsError(err.getMessage)
          }
        }
    }

    Json.format[P800Summary]
  }
}

case class TaxYearReconciliation(
  taxYear:        Int,
  reconciliation: P800Summary)

object TaxYearReconciliation {

  implicit val format: OFormat[TaxYearReconciliation] = Json.format[TaxYearReconciliation]
}
