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

package uk.gov.hmrc.mobilepaye.repository
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.mobilepaye.errors.MongoDBError
import uk.gov.hmrc.serviceResponse.Response

trait MongoHelper {

  protected def handleWriteResult[T](
    writeResult: WriteResult,
    document:    T
  ): Response[T] =
    if (writeResult.ok) Right(document) else Left(MongoDBError(resolveError(writeResult)))

  private def resolveError(writeResult: WriteResult): String =
    WriteResult
      .lastError(writeResult)
      .flatMap(lastError => lastError.errmsg.map(identity))
      .getOrElse("Unexpected error while writing a document.")
}
