/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package integrationBase

import java.time.LocalDateTime

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{MustMatchers, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import suite.WireMockHelper

trait SpecBase extends PlaySpec with GuiceOneAppPerSuite with TryValues with ScalaFutures with IntegrationPatience with WireMockHelper with MustMatchers with MockitoSugar {

  val aLocaDateTime = LocalDateTime.of(2020, 4, 1, 10, 30)
  def injector: Injector = app.injector

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("", "")

  val extraConfig: Map[String, Any] = {
    Map[String, Any]()
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
   // .configure(extraConfig)
    .build()

  protected def applicationBuilder(): GuiceApplicationBuilder = new GuiceApplicationBuilder()


}
