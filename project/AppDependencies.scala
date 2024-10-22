import sbt._

object AppDependencies {

  import play.sbt.PlayImport._

  private val playBootstrapVersion  = "9.5.0"
  private val playHmrcVersion       = "8.0.0"
  private val domainVersion         = "10.0.0"
  private val taxYearVersion        = "5.0.0"
  private val scalaMockVersion      = "5.2.0"
  private val hmrcMongoVersion      = "2.2.0"
  private val refinedVersion        = "0.11.2"
  private val authClientVersion     = "3.0.0"
  private val jsonExtensionsVersion = "0.42.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"    % playBootstrapVersion,
    "uk.gov.hmrc"       %% "domain-play-30"               % domainVersion,
    "uk.gov.hmrc"       %% "play-hmrc-api-play-30"        % playHmrcVersion,
    "uk.gov.hmrc"       %% "tax-year"                     % taxYearVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"           % hmrcMongoVersion,
    "eu.timepit"        %% "refined"                      % refinedVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % authClientVersion,
    "ai.x"              %% "play-json-extensions"         % jsonExtensionsVersion,
    ehcache
  )

  trait TestDependencies {
    lazy val scope: String        = "test, it"
    lazy val test:  Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock" %% "scalamock" % scalaMockVersion % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope)
      }.test
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion     % scope,
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % playBootstrapVersion % scope
  )

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
