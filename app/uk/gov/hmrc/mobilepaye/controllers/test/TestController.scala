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

package uk.gov.hmrc.mobilepaye.controllers.test

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.mobilepaye.domain.admin.FeatureFlagName
import uk.gov.hmrc.mobilepaye.repository.P800CacheMongo
import uk.gov.hmrc.mobilepaye.repository.admin.AdminRepository
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import uk.gov.hmrc.mobilepaye.domain.{P800Cache, P800CacheHashNino}


class TestController @Inject() (
  adminRepo: AdminRepository,
  p800CacheRepo: P800CacheMongo,
  val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendBaseController {

  def setFlag(
    flagName: FeatureFlagName,
    enabled: Boolean
  ): Action[AnyContent] = Action.async {
    adminRepo.setFeatureFlag(flagName, enabled).map {
      case true  => Created
      case false => NotFound
    }
  }

  def getP800Cache(nino: Nino): Action[AnyContent] = Action.async {
    p800CacheRepo.selectByNino(nino).map {
      case p800cache if p800cache.nonEmpty => Ok(Json.toJson(p800cache))
      case _                               => NotFound
    }
  }

  def addNino(nino: Nino, withHash: Boolean = false) = Action.async {
    for {
      p800cache <- p800CacheRepo.selectByNino(nino)
      _ = println("p800cache ::" + p800cache)
      res <- if (p800cache.nonEmpty) p800CacheRepo.deleteMany(nino) else Future.successful(true)
      _ = println("res ::" + res)
      p800cacheResponse <- p800CacheRepo.add(P800Cache(nino), withHash)
    } yield {
      p800cacheResponse match {
        case Left(value)  => println(" value is ::" + value); BadRequest
        case Right(value) => Ok(Json.toJson(value))
      }
    }

  }

  def delete(nino: Nino) = Action.async {
    p800CacheRepo.deleteMany(nino).map {
      case true  => Ok
      case false => NotFound
    }
  }
}
