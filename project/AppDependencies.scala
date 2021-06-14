import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "org.reactivemongo"       %% "play2-reactivemongo"             % "0.20.11-play27",
    "org.reactivemongo"       %% "reactivemongo-play-json-compat"  % "0.20.11-play27",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"       % "5.3.0",
    "com.typesafe.play"       %% "play-iteratees"                  % "2.6.1",
    "com.typesafe.play"       %% "play-json-joda"                  % "2.9.0",
    "org.typelevel"           %% "cats-core"                       % "2.1.1"
  )

  val test = Seq(
    "org.scalatest"               %% "scalatest"             % "3.1.0"   % "test, it",
    "com.typesafe.play"           %% "play-test"             % current   % "test, it",
    "org.pegdown"                 %  "pegdown"               % "1.6.0"   % "test, it",
    "org.scalatestplus.play"      %% "scalatestplus-play"    % "5.1.0"   % "test, it",
    "org.mockito"                 %% "mockito-scala"         % "1.10.6"  % "test, it",
    "com.vladsch.flexmark"        %  "flexmark-all"          % "0.35.10" % "test, it",
    "com.github.tomakehurst"      %  "wiremock-standalone"   % "2.25.0"  % "test, it",
    "org.scalatestplus"           %% "scalatestplus-scalacheck"  % "3.1.0.0-RC2"  % "test, it"
  )

}
