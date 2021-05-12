import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val play27Bootstrap    = "5.1.0"
  private val playHmrcApiVersion = "6.2.0-play-27"
  private val domainVersion      = "5.11.0-play-27"
  private val taxYearVersion     = "1.0.0"

  private val pegdownVersion             = "1.6.0"
  private val wireMockVersion            = "2.20.0"
  private val scalaMockVersion           = "4.1.0"
  private val scalaTestPlusVersion       = "4.0.3"
  private val simpleReactiveMongoVersion = "8.0.0-play-27"
  private val reactiveMongoTestVersion   = "5.0.0-play-27"
  private val mockitoVersion             = "2.25.0"
  private val refinedVersion             = "0.9.4"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-27" % play27Bootstrap,
    "uk.gov.hmrc" %% "domain"                    % domainVersion,
    "uk.gov.hmrc" %% "play-hmrc-api"             % playHmrcApiVersion,
    "uk.gov.hmrc" %% "tax-year"                  % taxYearVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo"      % simpleReactiveMongoVersion,
    "eu.timepit"  %% "refined"                   % refinedVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
    lazy val test:  Seq[ModuleID] = ???
  }

  object Test {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "org.scalamock" %% "scalamock"          % scalaMockVersion         % scope,
            "org.mockito"   % "mockito-core"        % mockitoVersion           % scope,
            "uk.gov.hmrc"   %% "reactivemongo-test" % reactiveMongoTestVersion % scope
          )
      }.test
  }

  object IntegrationTest {

    def apply(): Seq[ModuleID] =
      new TestDependencies {

        override lazy val scope: String = "it"

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
            "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope
          )
      }.test
  }

  private def testCommon(scope: String) = Seq(
    "org.pegdown"            % "pegdown"             % pegdownVersion       % scope,
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current  % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope
  )

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
