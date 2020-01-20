import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val play26Bootstrap    = "1.3.0"
  private val playHmrcApiVersion = "4.1.0-play-26"
  private val domainVersion      = "5.6.0-play-26"
  private val taxYearVersion     = "1.0.0"

  private val pegdownVersion             = "1.6.0"
  private val wireMockVersion            = "2.20.0"
  private val scalaMockVersion           = "4.1.0"
  private val scalaTestPlusVersion       = "3.1.2"
  private val simpleReactiveMongoVersion = "7.22.0-play-26"
  private val reactiveMongoTestVersion   = "4.15.0-play-26"
  private val mockitoVersion             = "2.25.0"
  private val refinedVersion             = "0.9.4"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26"    % play26Bootstrap,
    "uk.gov.hmrc" %% "domain"               % domainVersion,
    "uk.gov.hmrc" %% "play-hmrc-api"        % playHmrcApiVersion,
    "uk.gov.hmrc" %% "tax-year"             % taxYearVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion,
    "eu.timepit"  %% "refined"              % refinedVersion
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
