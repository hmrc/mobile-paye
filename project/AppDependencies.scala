import sbt._

object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val play25Bootstrap = "3.1.0"
  private val playHmrcApiVersion = "2.1.0"
  private val domainVersion = "5.2.0"

  private val hmrcTestVersion = "3.0.0"
  private val pegdownVersion = "1.6.0"
  private val scalaTestVersion = "3.0.4"
  private val wireMockVersion = "2.9.0"
  private val scalaMockVersion = "4.0.0"
  private val scalaTestPlusVersion = "2.0.1"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % play25Bootstrap,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "play-hmrc-api" % playHmrcApiVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
        "org.scalamock" %% "scalamock" % scalaMockVersion % scope

      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test: Seq[ModuleID] = testCommon(scope) ++ Seq(
        "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope
      )
    }.test
  }

  private def testCommon(scope: String) = Seq(
    "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
    "org.pegdown" % "pegdown" % pegdownVersion % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope
  )

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}