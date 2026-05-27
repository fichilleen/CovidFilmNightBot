import services.FetchFilmInfo
import akka.actor.ActorSystem
import org.scalatest.flatspec._
import org.scalatest.matchers._

class FetchFilmInfoTest extends AnyFlatSpec with must.Matchers {

  implicit val as: ActorSystem = ActorSystem("tests")
  val klass = new FetchFilmInfo("xxx")

  "urlParamsFromTitle" must "return the base name without a year" in {
    klass.urlParamsFromInput("jaws") must be("t=jaws")
  }

  "urlParamsFromTitle" must "convert to lower case" in {
    klass.urlParamsFromInput("Jaws") must be("t=jaws")
  }

  "urlParamsFromTitle" must "replace spaces with plusses" in {
    klass.urlParamsFromInput("natural born killers") must be("t=natural+born+killers")
  }

  "urlParamsFromTitle" must "escape ampersands" in {
    klass.urlParamsFromInput("bill&ted") must be("t=bill%26ted")
  }

  "urlParamsFromTitle" must "separate year into separate parameter if it exists" in {
    klass.urlParamsFromInput("jaws (1975)") must be("t=jaws&y=1975")
  }

  "urlParamsFromTitle" must "strip any trailing space" in {
    klass.urlParamsFromInput("jaws ") must be("t=jaws")
  }

  "urlParamsFromTitle" must "parse imdb urls" in {
    klass.urlParamsFromInput("https://www.imdb.com/title/tt011116/") must be("i=tt011116")
    klass.urlParamsFromInput("https://www.imdb.com/title/tt0111161/") must be("i=tt0111161")
    klass.urlParamsFromInput("https://www.imdb.com/title/tt01111617/") must be("i=tt01111617")
  }
}
