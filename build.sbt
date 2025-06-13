import play.sbt.PlayImport.PlayKeys.*
import sbt.Keys.excludeDependencies

import scala.collection.Seq

val appName: String = "mobile-paye"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    Seq(play.sbt.PlayScala, SbtDistributablesPlugin, ScoverageSbtPlugin): _*
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.domain._",
      "uk.gov.hmrc.mobilepaye.binders.Binders._",
      "uk.gov.hmrc.time.TaxYear",
      "uk.gov.hmrc.mobilepaye.domain.types._",
      "uk.gov.hmrc.mobilepaye.domain.admin._",
      "uk.gov.hmrc.mobilepaye.domain.types.JourneyId._"
    )
  )
  .settings(
    majorVersion := 0,
    scalaVersion := "3.3.5",
    playDefaultPort := 8247,
    libraryDependencies ++= AppDependencies(),
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers += Resolver.jcenterRepo,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it", base / "test-common")).value,
    Test / unmanagedSourceDirectories := (Test / baseDirectory)(base => Seq(base / "test", base / "test-common")).value,
    IntegrationTest / parallelExecution := false,
    scalacOptions in Test -= "-Ywarn-dead-code",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Ywarn-dead-code",
      "-Ywarn-value-discard",
      "-Ywarn-numeric-widen",
      "-Xlint",
      "-Wconf:cat=w-flag-value-discard&msg=discarded non-Unit value of type org\\.scalatest\\.Assertion:s"
    ),
    coverageMinimumStmtTotal := 90,
    coverageFailOnMinimum := true,
    coverageHighlighting := true,
    coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;.*BuildInfo.*;.*Routes.*;.*javascript.*;.*Reverse.*",
    excludeDependencies ++= Seq(
      // As of Play 3.0, groupId has changed to org.playframework; exclude transitive dependencies to the old artifacts
      // Specifically affects play-json-extensions dependency
      ExclusionRule(organization = "com.typesafe.play")
    )
  )
