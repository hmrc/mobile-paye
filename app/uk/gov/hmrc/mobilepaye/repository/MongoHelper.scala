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

package uk.gov.hmrc.mobilepaye.repository
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.JSONSerializationPack.Reader
import reactivemongo.play.json.commands.JSONFindAndModifyCommand.FindAndModifyResult
import uk.gov.hmrc.mobilepaye.errors.{MongoDBError, MongoDBNoResults}
import uk.gov.hmrc.serviceResponse.Response

trait MongoHelper {

  protected def resolveUpdateResult[T](dbResult: FindAndModifyResult)(implicit reader: Reader[T]): Response[T] = {
    val result = dbResult.result[T]
    val error  = dbResult.lastError

    (error, result) match {
      case (_, Some(value)) => Right(value)
      case (Some(err), _) =>
        Left(err.err.map(MongoDBError).getOrElse(MongoDBNoResults("No document to update was found.")))
      case _ => Left(MongoDBError("Unexpected error while updating a document."))
    }
  }

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
