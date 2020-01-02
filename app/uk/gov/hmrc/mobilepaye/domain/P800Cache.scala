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

package uk.gov.hmrc.mobilepaye.domain

import org.joda.time.DateTime
import play.api.libs.functional.syntax.{unlift, _}
import play.api.libs.json.{OFormat, OWrites, Reads, _}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class P800Cache(
  nino:      Nino,
  createdAt: DateTime = new DateTime())

object P800Cache {

  private implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats

  def defaultReads: Reads[P800Cache] =
    (__ \ "nino")
      .read[Nino]
      .and((__ \ "createdAt").read[DateTime])(P800Cache.apply(_, _))

  def defaultWrites: OWrites[P800Cache] =
    (__ \ "nino")
      .write[Nino]
      .and((__ \ "createdAt").write[DateTime])(unlift(P800Cache.unapply))

  implicit val format: OFormat[P800Cache] = OFormat(defaultReads, defaultWrites)

}
