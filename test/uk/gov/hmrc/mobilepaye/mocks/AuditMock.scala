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

package uk.gov.hmrc.mobilepaye.mocks

import org.scalamock.matchers.MatcherBase
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.JsValue
import play.api.libs.json.Json.obj
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.domain.{MobilePayeResponse, MobilePayeResponseAudit}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.{ExecutionContext, Future}

trait AuditMock extends MockFactory {

  def dataEventWith(
    auditSource:     String,
    auditType:       String,
    transactionName: String,
    detail:          JsValue
  ): MatcherBase =
    argThat { (dataEvent: ExtendedDataEvent) =>
      dataEvent.auditSource.equals(auditSource) &&
      dataEvent.auditType.equals(auditType) &&
      dataEvent.tags("transactionName").equals(transactionName) &&
      dataEvent.tags.get("path").isDefined &&
      dataEvent.tags.get("clientIP").isDefined &&
      dataEvent.tags.get("clientPort").isDefined &&
      dataEvent.tags.get("X-Request-ID").isDefined &&
      dataEvent.tags.get("X-Session-ID").isDefined &&
      dataEvent.tags.get("Unexpected").isEmpty &&
      dataEvent.detail.equals(detail)
    }

  def mockAudit(
    nino:                    Nino,
    expectedDetails:         MobilePayeResponseAudit,
    journeyId:               String
  )(implicit auditConnector: AuditConnector
  ): Unit = {
    (auditConnector
      .sendExtendedEvent(_: ExtendedDataEvent)(_: HeaderCarrier, _: ExecutionContext))
      .expects(dataEventWith("mobile-paye",
                             "viewPayeSummary",
                             "view-paye-summary",
                             obj("nino" -> nino.value, "journeyId" -> journeyId, "data" -> expectedDetails)),
               *,
               *)
      .returning(Future successful Success)
    ()
  }

}
