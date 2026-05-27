package services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import models.OmdbapiResponses

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.matching.Regex

class FetchFilmInfo(apiKey: String)(implicit as: ActorSystem, mat: Materializer) {
  import models.FilmResponseImplicits._
  implicit val dispatcher: ExecutionContextExecutor = as.dispatcher

  private val filmWithYearRegex: Regex = "(.*) \\(([0-9]{4})\\)".r
  private val imdbUrl: Regex = """https://www\.imdb.com/title/(tt[0-9]{6,10})""".r

  final val baseUrl: String = s"http://www.omdbapi.com/?apikey=$apiKey"

  def fetchFilmInfo(title: String): Future[OmdbapiResponses] = {
    val requestUri = s"$baseUrl&${urlParamsFromInput(title)}"
    val request: HttpRequest = Get(requestUri)
    val response = Http().singleRequest(request)
    println(s"Called the api with $requestUri")

    response.flatMap(r =>
      Unmarshal(r).to[OmdbapiResponses]
    )
  }

  def urlParamsFromInput(title: String): String = {
    def normaliseTitle(title: String): String =
      title
        .toLowerCase
        .trim
        .replaceAll(" ", "+")
        .replaceAll("&", "%26")

    lazy val matchesImdb = imdbUrl.findAllIn(title)
    val hasYear = filmWithYearRegex.findAllIn(title)
    if (hasYear.nonEmpty) {
      s"t=${normaliseTitle(hasYear.group(1))}&y=${hasYear.group(2)}"
    }
    else if(matchesImdb.nonEmpty) s"i=${matchesImdb.group(1)}"
    else s"t=${normaliseTitle(title)}"
  }

}
