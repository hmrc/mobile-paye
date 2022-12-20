package repository.admin

import akka.util.Timeout
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.{Seconds, Span}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, running}
import uk.gov.hmrc.mobilepaye.domain.admin.{FeatureFlag, OnlinePaymentIntegration}
import uk.gov.hmrc.mobilepaye.repository.admin.AdminRepository

import scala.concurrent.ExecutionContext.Implicits.global

class AdminRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with OptionValues
    with IntegrationPatience {

  "Admin data repo" - {
    "getFeatureFlag must return None if there is no record" in {
      val app = new GuiceApplicationBuilder().build()

      running(app) {
        val repo = app.injector.instanceOf[AdminRepository]

        whenReady(
          repo
            .collection
            .drop()
            .toFuture()
            .flatMap(
              _ =>
                repo
                  .getFeatureFlag(OnlinePaymentIntegration)
            )
        ) {
          result =>
            result mustBe None
        }
      }
    }

    "setFeatureFlag must replace a record not create a new one" in {
      val app = new GuiceApplicationBuilder().build()

      running(app) {
        val repo = app.injector.instanceOf[AdminRepository]

        whenReady(
          repo
            .collection
            .drop()
            .toFuture()
            .flatMap(
              _ =>
                for {
                  _   <- repo.setFeatureFlag(OnlinePaymentIntegration, enabled = true)
                  _   <- repo.setFeatureFlag(OnlinePaymentIntegration, enabled = false)
                  res <- repo.collection.find(Filters.equal("name", OnlinePaymentIntegration.toString)).toFuture()
                } yield res
            )
        ) {
          result =>
            result.length mustBe 1
            result.head.isEnabled mustBe false
        }
      }
    }

    "getAllFeatureFlags must get a list of all the feature toggles" in {
      val app = new GuiceApplicationBuilder().build()

      running(app) {
        val repo = app.injector.instanceOf[AdminRepository]

        whenReady(
          repo
            .collection
            .drop()
            .toFuture()
            .flatMap(
              _ =>
                for {
                  _   <- repo.setFeatureFlag(OnlinePaymentIntegration, enabled = true)
                  res <- repo.getFeatureFlags
                } yield res
            )
        ) {
          result =>
            result mustBe List(
              FeatureFlag(
                name        = OnlinePaymentIntegration,
                isEnabled   = true,
                description = OnlinePaymentIntegration.description
              )
            )
        }
      }
    }

    "must set/get feature flags correctly" in {
      val app = new GuiceApplicationBuilder().build()

      running(app) {

        val repo = app.injector.instanceOf[AdminRepository]

        repo.collection.drop()

        val flag: FeatureFlag =
          FeatureFlag(
            name        = OnlinePaymentIntegration,
            isEnabled   = true,
            description = Some("Enable/disable online payment integration")
          )

        val data: List[FeatureFlag] =
          List(flag)

        whenReady(
          repo
            .setFeatureFlags(Map(flag.name -> flag.isEnabled))
            .flatMap(_ => repo.getFeatureFlags)
        ) {
            result =>
              result mustBe data
          }
      }
    }

    "not allow duplicates" in {
      val app = new GuiceApplicationBuilder().build()

      running(app) {
        implicit val timeout: Timeout = Timeout(Span(5.00, Seconds))

        val repo = app.injector.instanceOf[AdminRepository]

        val result = intercept[MongoWriteException] {
          await(for {
            _      <- repo.collection.insertOne(FeatureFlag(OnlinePaymentIntegration, isEnabled = true)).toFuture()
            result <- repo.collection.insertOne(FeatureFlag(OnlinePaymentIntegration, isEnabled = false)).toFuture()
          } yield result)
        }

        result.getCode mustBe 11000
        result.getError.getMessage mustBe
          s"""E11000 duplicate key error collection: mobile-paye.admin-feature-flags index: name dup key: { name: "$OnlinePaymentIntegration" }"""
      }
    }
  }
}
