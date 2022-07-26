import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"              % "0.68.0",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"       % "5.3.0",
    "com.typesafe.play"       %% "play-iteratees"                  % "2.6.1",
    "org.typelevel"           %% "cats-core"                       % "2.1.1"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"                % "3.1.0",
    "com.typesafe.play"           %% "play-test"                % current,
    "org.pegdown"                 %  "pegdown"                  % "1.6.0",
    "org.scalatestplus.play"      %% "scalatestplus-play"       % "5.1.0",
    "org.mockito"                 %% "mockito-scala"            % "1.10.6",
    "com.vladsch.flexmark"        %  "flexmark-all"             % "0.35.10",
    "com.github.tomakehurst"      %  "wiremock-standalone"      % "2.25.0",
    "org.scalatestplus"           %% "scalatestplus-scalacheck" % "3.1.0.0-RC2",
    "uk.gov.hmrc.mongo"           %% "hmrc-mongo-test-play-28"  % "0.68.0"
  ).map(_ % Test)

}
