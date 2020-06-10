import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "org.reactivemongo" %% "play2-reactivemongo"           % "0.18.6-play26",
    "uk.gov.hmrc"             %% "simple-reactivemongo"     % "7.27.0-play-26",
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.8.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-play-26"        % "1.8.0" % Test classifier "tests",
    "org.scalatest"           %% "scalatest"                % "3.0.8"                 % "test",
    "com.typesafe.play"       %% "play-test"                % current                 % "test, it",
    "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "3.1.2"                 % "test, it",
    "org.mockito"                 %  "mockito-all"           % "1.10.19"              % "test, it",
    "com.github.tomakehurst"      %  "wiremock-standalone"   % "2.25.0"               %"test, it",
    "org.scalacheck"              %% "scalacheck"            % "1.14.0"               %"test, it",
    "uk.gov.hmrc"                 %% "hmrctest"              % "3.9.0-play-26"        %"test, it"


  )

}
