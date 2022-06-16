import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val play28Bootstrap = "5.24.0"
  private val playHmrcVersion = "7.0.0-play-28"
  private val domainVersion   = "8.1.0-play-28"
  private val taxYearVersion  = "1.6.0"

  private val pegdownVersion             = "1.6.0"
  private val wireMockVersion            = "2.20.0"
  private val scalaMockVersion           = "4.1.0"
  private val scalaTestPlusVersion       = "5.1.0"
  private val simpleReactiveMongoVersion = "8.0.0-play-28"
  private val reactiveMongoTestVersion   = "5.0.0-play-28"
  private val mockitoVersion             = "1.16.46"
  private val refinedVersion             = "0.9.4"
  private val flexmarkAllVersion         = "0.36.8"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % play28Bootstrap,
    "uk.gov.hmrc" %% "domain"                    % domainVersion,
    "uk.gov.hmrc" %% "play-hmrc-api"             % playHmrcVersion,
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
            "org.mockito"   % "mockito-scala_2.12"  % mockitoVersion           % scope,
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
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "com.vladsch.flexmark"   % "flexmark-all"        % flexmarkAllVersion   % scope
  )

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
