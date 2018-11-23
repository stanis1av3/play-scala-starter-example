import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._

/**
 * Functional tests start a Play application internally, available
 * as `app`.
 */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite {

  "Routes" should {

    "send 404 on a bad request" in  {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }

    "send 200 on a good request" in  {
      route(app, FakeRequest(GET, "/")).map(status(_)) mustBe Some(OK)
    }

  }

  "ExchangeRateController" should {

    "render the index page" in {
      val home = route(app, FakeRequest(GET, "/")).get

      status(home) mustBe Status.OK
      contentType(home) mustBe Some("text/html")
      contentAsString(home) must include ("Please go to")
    }

  }

  "ExchangeRateController" should {

    "return an increasing count" in {
      contentAsString(route(app, FakeRequest(GET, "/rates/EUR/EUR/1")).get) mustBe "1"
      contentAsString(route(app, FakeRequest(GET, "/rates/USD/USD/2000")).get) mustBe "2000"
    }

  }
}
