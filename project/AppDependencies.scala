import sbt._
import play.sbt.PlayImport.ehcache

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val play28Bootstrap = "7.19.0"
  private val playHmrcVersion = "7.2.0-play-28"
  private val domainVersion   = "8.1.0-play-28"
  private val taxYearVersion  = "3.0.0"

  private val pegdownVersion       = "1.6.0"
  private val wireMockVersion      = "2.21.0"
  private val scalaMockVersion     = "5.1.0"
  private val scalaTestPlusVersion = "5.1.0"
  private val hmrcMongoVersion     = "0.73.0"
  private val mockitoVersion       = "1.17.30"
  private val refinedVersion       = "0.9.26"
  private val flexmarkAllVersion   = "0.36.8"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28"    % play28Bootstrap,
    "uk.gov.hmrc"       %% "domain"                       % domainVersion,
    "uk.gov.hmrc"       %% "play-hmrc-api"                % playHmrcVersion,
    "uk.gov.hmrc"       %% "tax-year"                     % taxYearVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"           % hmrcMongoVersion,
    "eu.timepit"        %% "refined"                      % refinedVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-28" % "1.2.0",
    "ai.x"              %% "play-json-extensions"         % "0.42.0",
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
            "org.scalamock" %% "scalamock"         % scalaMockVersion % scope,
            "org.mockito"   %% "mockito-scala"       % mockitoVersion   % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.mockito"            %% "mockito-scala" % mockitoVersion  % scope,
            "com.github.tomakehurst" % "wiremock"           % wireMockVersion % scope
          )
      }.test
  }

  private def testCommon(scope: String) = Seq(
    "org.pegdown"            % "pegdown"                  % pegdownVersion       % scope,
    "com.typesafe.play"      %% "play-test"               % PlayVersion.current  % scope,
    "org.scalatestplus.play" %% "scalatestplus-play"      % scalaTestPlusVersion % scope,
    "com.vladsch.flexmark"    % "flexmark-all"             % flexmarkAllVersion   % scope,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % hmrcMongoVersion     % scope,
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % play28Bootstrap      % scope
  )

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
