/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mobilepaye.domain.P800Repayment
import uk.gov.hmrc.mobilepaye.domain.taxcalc.P800Status.Overpaid
import uk.gov.hmrc.mobilepaye.domain.taxcalc.RepaymentStatus.{Refund, SaUser, UnableToClaim}
import uk.gov.hmrc.time.TaxYear

case class P800Summary(
                        p800_status:   P800Status,
                        paymentStatus: RepaymentStatus,
                        amount:        BigDecimal,
                        taxYear:       Int,
                        datePaid:      Option[String]
                      )

object P800Summary {
  def toP800Repayment(p800Summary: P800Summary): Option[P800Repayment] = {
    def withOverpaidP800(p800Summary: P800Summary): Option[P800Summary] = {
      p800Summary.p800_status match {
        case Overpaid => Option(p800Summary)
        case _        => None
      }
    }

    def withAcceptableRepaymentStatus(p800Summary: P800Summary): Option[P800Summary] = {
      p800Summary.paymentStatus match {
          case SaUser        => None
          case UnableToClaim => None
          case _             => Option(p800Summary)
        }
    }

    def transform(p800Summary: P800Summary): P800Repayment = {
      def withLink: Option[String] = {
        p800Summary.paymentStatus match {
          case Refund => Option(s"/tax-you-paid/${TaxYear.current.currentYear - 1}-${TaxYear.current.currentYear}/paid-too-much")
          case _      => None
        }}

      P800Repayment(p800Summary.amount, p800Summary.paymentStatus, p800Summary.datePaid, p800Summary.taxYear, withLink)
    }


    for {
      _      <- withOverpaidP800(p800Summary)
      _      <- withAcceptableRepaymentStatus(p800Summary)
      result =  transform(p800Summary)
    } yield result
  }

  implicit val format: Format[P800Summary] = Json.format
}
