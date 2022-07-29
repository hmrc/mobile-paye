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

package uk.gov.hmrc.mobilepaye.utils

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.{Matchers, WordSpecLike}
import play.api.cache.AsyncCacheApi
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mobilepaye.MobilePayeTestData
import uk.gov.hmrc.mobilepaye.mocks.{AuditMock, AuthorisationMock, ShutteringMock}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait BaseSpec
    extends WordSpecLike
    with Matchers
    with FutureAwaits
    with DefaultAwaitTimeout
    with MobilePayeTestData
    with AuthorisationMock
    with AuditMock
    with ShutteringMock {
  implicit lazy val ec:           ExecutionContext  = scala.concurrent.ExecutionContext.Implicits.global
  implicit lazy val hc:           HeaderCarrier     = HeaderCarrier()
  implicit lazy val system:       ActorSystem       = ActorSystem()
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer()

  val acceptHeader: (String, String) = "Accept" -> "application/vnd.hmrc.1.0+json"

  // A cache that doesn't cache
  val mockCacheApi: AsyncCacheApi = new AsyncCacheApi {
    override def set(key: String, value: Any, expiration: Duration): Future[Done] = ???

    override def remove(key: String): Future[Done] = ???

    override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: => Future[A])(implicit evidence$1: ClassTag[A]): Future[A] = orElse

    override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Future[Option[T]] = ???

    override def removeAll(): Future[Done] = ???
  }
}
