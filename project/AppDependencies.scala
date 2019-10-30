import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val play26Bootstrap    = "0.36.0"
  private val playHmrcApiVersion = "3.4.0-play-26"
  private val domainVersion      = "5.6.0-play-26"
  private val taxYearVersion     = "0.5.0"

  private val pegdownVersion             = "1.6.0"
  private val wireMockVersion            = "2.20.0"
  private val scalaMockVersion           = "4.1.0"
  private val scalaTestPlusVersion       = "3.1.2"
  private val simpleReactiveMongoVersion = "7.20.0-play-26"
  private val embedVersion               = "1.48.0"
  private val embedMongoVersion          = "0.2.2"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26"    % play26Bootstrap,
    "uk.gov.hmrc" %% "domain"               % domainVersion,
    "uk.gov.hmrc" %% "play-hmrc-api"        % playHmrcApiVersion,
    "uk.gov.hmrc" %% "tax-year"             % taxYearVersion,
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion
  )

  trait TestDependencies {
    lazy val scope: String        = "test"
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

        override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
          "com.github.tomakehurst" % "wiremock"                  % wireMockVersion   % scope,
          "de.flapdoodle.embed"    % "de.flapdoodle.embed.mongo" % embedVersion      % scope,
          "com.github.simplyscala" %% "scalatest-embedmongo"     % embedMongoVersion % scope
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
