package services

package services

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.iteratee.Enumerator
import play.api.test.Helpers._
import suite.MongoSuite

import scala.concurrent.ExecutionContext.Implicits.global

class GridFSStorageServiceSpec extends FreeSpec
  with MustMatchers
  with ScalaFutures
  with OptionValues
  with MongoSuite
  with IntegrationPatience {

  "GridFS Storage Service" - {

    "must store an xml file" in {

      val app = new GuiceApplicationBuilder().build()

      running(app) {

        val storageService = app.injector.instanceOf[GridFSStorageService]

        database.flatMap(_.drop()).futureValue

        val xml =
          """
            |<Test>
            |    <Result>Success</Result>
            |</Test>
            |""".stripMargin

        val stream: InputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        val enumeration = Enumerator.fromStream(stream)

        whenReady(storageService.writeFileToGridFS("testFile.xml", enumeration)) { result =>
          result mustBe true
        }
      }
    }
  }
}
