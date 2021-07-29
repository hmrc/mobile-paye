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

package uk.gov.hmrc.mobilepaye.domain

import play.api.libs.json.{Json, OFormat}

case class PayeIncomeAudit(
                            name:               String,
                            taxCode:            String,
                            amount:             BigDecimal,
                            latestPaymentAudit: Option[LatestPaymentAudit])

object PayeIncomeAudit {

  def fromPayeIncome(payeIncome: PayeIncome): PayeIncomeAudit =
    PayeIncomeAudit(
      name               = payeIncome.name,
      taxCode            = payeIncome.taxCode,
      amount             = payeIncome.amount,
      latestPaymentAudit = LatestPaymentAudit.fromLatestPayment(payeIncome.latestpayment)
    )

  implicit val format: OFormat[PayeIncomeAudit] = Json.format[PayeIncomeAudit]
}