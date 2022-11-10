import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"              % "0.73.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"       % "7.11.0",
    "org.typelevel"           %% "cats-core"                       % "2.1.1"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"                % "3.2.14",
    "org.scalatestplus"           %% "scalacheck-1-17"          % "3.2.14.0",
    "com.typesafe.play"           %% "play-test"                % current,
    "org.pegdown"                 %  "pegdown"                  % "1.6.0",
    "org.scalatestplus.play"      %% "scalatestplus-play"       % "5.1.0",
    "org.mockito"                 %% "mockito-scala"            % "1.17.12",
    "com.vladsch.flexmark"        %  "flexmark-all"             % "0.35.10",
    "com.github.tomakehurst"      %  "wiremock-standalone"      % "2.25.0",
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-28"  % "0.68.0",
    "com.vladsch.flexmark"        %  "flexmark-all"             % "0.64.0"
  ).map(_ % Test)

}
