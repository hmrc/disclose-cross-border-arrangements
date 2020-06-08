import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.SbtArtifactory
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "disclose-cross-border-arrangements"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    majorVersion                     := 0,
    libraryDependencies              ++= AppDependencies.compile ++ AppDependencies.test
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings): _*)
  .settings(resolvers += Resolver.jcenterRepo)

lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "it"
  ),
  unmanagedResourceDirectories := Seq(
    baseDirectory.value / "it" / "resources"
  ),
  parallelExecution := false,
  fork              := true,
  javaOptions ++= Seq(
    "-Dconfig.resource=it.application.conf",
    "-Dlogger.resource=it.logback.xml"
  )
)