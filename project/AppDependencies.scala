import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "org.reactivemongo"       %% "play2-reactivemongo"        % "0.20.11-play27",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-27"  % "2.24.0",
    "com.typesafe.play"       %% "play-iteratees"             % "2.6.1",
    "com.typesafe.play"       %% "play-json-joda"             % "2.9.0",
    "org.reactivemongo"       %% "reactivemongo-play-json-compat" % "0.20.12-play27"
  )

  val test = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-play-26"     % "1.8.0" % Test classifier "tests",
    "org.scalatest"               %% "scalatest"             % "3.0.8"                % "test, it",
    "com.typesafe.play"           %% "play-test"             % current                % "test, it",
    "org.pegdown"                 %  "pegdown"               % "1.6.0"                % "test, it",
    "org.scalatestplus.play"      %% "scalatestplus-play"    % "4.0.3"                % "test, it",
    "org.mockito"                 %  "mockito-all"           % "1.10.19"              % "test, it",
    "com.github.tomakehurst"      %  "wiremock-standalone"   % "2.25.0"               % "test, it",
    "org.scalacheck"              %% "scalacheck"            % "1.14.0"               % "test, it"
  )

}
