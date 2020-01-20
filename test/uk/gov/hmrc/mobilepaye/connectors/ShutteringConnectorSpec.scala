/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.mobilepaye.connectors

import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilepaye.domain.Shuttering
import uk.gov.hmrc.mobilepaye.utils.BaseSpec
import eu.timepit.refined.auto._

import scala.concurrent.{ExecutionContext, Future}

class ShutteringConnectorSpec extends BaseSpec {
  val mockCoreGet: CoreGet             = mock[CoreGet]
  val connector:   ShutteringConnector = new ShutteringConnector(mockCoreGet, "")

  def mockShutteringGet[T](f: Future[T]) =
    (mockCoreGet
      .GET(_: String)(_: HttpReads[T], _: HeaderCarrier, _: ExecutionContext))
      .expects("/mobile-shuttering/service/mobile-paye/shuttered-status?journeyId=27085215-69a4-4027-8f72-b04b10ec16b0",
               *,
               *,
               *)
      .returning(f)

  "getTaxReconciliations" should {
    "Assume unshuttered for InternalServerException response" in {
      mockShutteringGet(Future.successful(new InternalServerException("")))

      val result: Shuttering = await(connector.getShutteringStatus("27085215-69a4-4027-8f72-b04b10ec16b0"))
      result shouldBe Shuttering.shutteringDisabled
    }

    "Assume unshuttered for BadGatewayException response" in {
      mockShutteringGet(Future.successful(new BadGatewayException("")))

      val result: Shuttering = await(connector.getShutteringStatus("27085215-69a4-4027-8f72-b04b10ec16b0"))
      result shouldBe Shuttering.shutteringDisabled
    }
  }
}
