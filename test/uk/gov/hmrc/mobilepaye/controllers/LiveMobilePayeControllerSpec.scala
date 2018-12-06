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

import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mobilepaye.domain.MobilePayeResponse
import uk.gov.hmrc.mobilepaye.domain.tai.Person
import uk.gov.hmrc.mobilepaye.services.MobilePayeService
import uk.gov.hmrc.mobilepaye.utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}


class LiveMobilePayeControllerSpec extends BaseSpec {

  val fakeRequest = FakeRequest("GET", "/")

  val mockMobilePayeService = mock[MobilePayeService]

  val controller = new LiveMobilePayeController(mockMobilePayeService)

  def mockGetMobilePayeResponse(f: Future[MobilePayeResponse]): Unit = {
    (mockMobilePayeService.getMobilePayeResponse(_: Nino)
    (_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *).returning(f)
  }

  def mockGetPerson(f: Future[Person]): Unit = {
    (mockMobilePayeService.getPerson(_: Nino)
    (_: HeaderCarrier, _: ExecutionContext)).expects(*, *, *).returning(f)
  }


  s"GET /$nino/summary/current-income" should {
    "return 200 and full paye summary data for valid authorised nino" in {
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.successful(fullMobilePayeResponse))

      val result = await(controller.getPayeSummary(nino)(fakeRequest))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(fullMobilePayeResponse)
    }

    "return 200 and paye summary data with no employment data for valid authorised nino" in {
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.successful(fullMobilePayeResponse.copy(employments = None)))

      val result = await(controller.getPayeSummary(nino)(fakeRequest))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(fullMobilePayeResponse.copy(employments = None))
    }

    "return 200 and paye summary data with no pensions data for valid authorised nino" in {
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.successful(fullMobilePayeResponse.copy(pensions = None)))

      val result = await(controller.getPayeSummary(nino)(fakeRequest))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(fullMobilePayeResponse.copy(pensions = None))
    }

    "return 200 and paye summary data with no other income data for valid authorised nino" in {
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.successful(fullMobilePayeResponse.copy(otherIncomes = None)))

      val result = await(controller.getPayeSummary(nino)(fakeRequest))

      status(result) shouldBe 200
      jsonBodyOf(result) shouldBe Json.toJson(fullMobilePayeResponse.copy(otherIncomes = None))
    }

    "return 404 when there is no employment, pension or otherIncome data found for valid authorised nino" in {
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.successful(fullMobilePayeResponse.copy(employments = None, pensions = None, otherIncomes = None)))

      val result = await(controller.getPayeSummary(nino)(fakeRequest))

      status(result) shouldBe 404
    }

    "return 423 for a valid nino and authorised user but corrupt/mcierror user" in {
      mockGetPerson(Future.successful(person.copy(hasCorruptData = true)))

      val result = await(controller.getPayeSummary(nino)(fakeRequest))

      status(result) shouldBe 423
    }

    "return 410 for a valid nino and authorised user but deceased user" in {
      mockGetPerson(Future.successful(person.copy(isDeceased = true)))

      val result = await(controller.getPayeSummary(nino)(fakeRequest))

      status(result) shouldBe 410
    }

    "return 500 when MobilePayeService throws an InternalServerException" in {
      mockGetPerson(Future.successful(person))
      mockGetMobilePayeResponse(Future.failed(new InternalServerException("Internal Server Error")))

      val result = await(controller.getPayeSummary(nino)(fakeRequest))

      status(result) shouldBe 500
    }

    "return 401 for valid nino but unauthorized user" in {
      pending
    }

    "return 403 for valid nino for authorised user but for a different nino" in {
      pending
    }

  }

}
