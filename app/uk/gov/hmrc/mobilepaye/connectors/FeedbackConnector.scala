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

package uk.gov.hmrc.mobilepaye.connectors

import uk.gov.hmrc.http._
import uk.gov.hmrc.mobilepaye.domain.Feedback

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeedbackConnector @Inject()( http: HttpClient, @Named("mobile-feedback") baseUrl: String)(implicit ex: ExecutionContext){

  def postFeedback(feedback: Feedback)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = baseUrl + s"/mobile-feedback/feedback/mobile-paye"
    http.POST[Feedback, HttpResponse](url,feedback)
    }
  }
