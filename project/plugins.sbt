resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)
resolvers += Resolver.typesafeRepo("releases")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.20.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.4.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin"         % "2.8.21")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "1.9.3")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"        % "0.3.4")
