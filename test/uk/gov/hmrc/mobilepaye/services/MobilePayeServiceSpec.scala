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

package uk.gov.hmrc.mobilepaye.services

import uk.gov.hmrc.mobilepaye.connectors.TaiConnector
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.Future


class MobilePayeServiceSpec extends BaseSpec {

  val mockTaiConnector = mock[TaiConnector]

  val service = new MobilePayeService(mockTaiConnector)

  "getMobilePayeResponse" should {
    "return full MobilePayeResponse when all data is available" in {
      val result = service.getMobilePayeResponse(nino)

      result shouldBe Future.successful(fullMobilePayeResponse)
    }

    "return MobilePayeResponse with no employments when employment data is missing" in {
      pending
    }

    "return MobilePayeResponse with no pensions when pension data is missing" in {
      pending
    }

    "return MobilePayeResponse with no otherIncomes when OtherIncome data is missing" in {
      pending
    }

    "throw NotAuthorisedException when receiving NotAuthorisedException from taiConnector" in {
      pending
    }

    "throw ForbiddenException when receiving ForbiddenException from taiConnector" in {
      pending
    }

    "throw InternalServerError when receiving InternalServerError from taiConnector" in {
      pending
    }
  }
}