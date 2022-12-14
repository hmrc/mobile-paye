package repository.admin

import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.running
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
  }
}
