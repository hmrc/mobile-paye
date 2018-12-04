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

import play.api.test.FakeRequest
import uk.gov.hmrc.mobilepaye.utils.BaseSpec


class LiveMobilePayeControllerSpec extends BaseSpec {

  val fakeRequest = FakeRequest("GET", "/")

  s"GET /$nino/summary/current-income" should {
    "return 200 and full paye summary data for valid authorised nino" in {
      pending
    }

    "return 200 and paye summary data with no employment data for valid authorised nino" in {
      pending
    }

    "return 200 and paye summary data with no pensions data for valid authorised nino" in {
      pending
    }

    "return 200 and paye summary data with no other income data for valid authorised nino" in {
      pending
    }

    "return 200 and paye summary data with no employment, pensions or other income data for valid authorised nino" in {
      pending
    }

    "return 401 for valid nino but unauthorized user" in {
      pending
    }

    "return 403 for valid nino for authorised user but for a different nino" in {
      pending
    }

    "return 500 when MobilePayeService throws an InternalServerErrorException" in {
      pending
    }

    "return 404 when no user data is found for an authorised user with a valid nino" in {
      pending
    }

    "return 423 for a valid nino and authorised user but corrupt/mcierror user" in {
      //TODO verify no data calls made other than person details
      pending
    }

    "return 410 for a valid nino and authorised user but deceased user" in {
      //TODO verify no data calls made other than person details
      pending
    }

  }

}
