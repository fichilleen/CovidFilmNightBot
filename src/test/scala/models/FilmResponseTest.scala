package models
import org.scalatest.flatspec._
import org.scalatest.matchers._
import spray.json._

class FilmResponseText extends AnyFlatSpec with must.Matchers {

  "initialization" should "be possible from JSON" in new ActualJson {
    import FilmResponseImplicits._
    val parsed: FilmResponse = actualJson.parseJson.convertTo[FilmResponse]
    parsed.title must be("Natural Born Killers")
 }

  "asMessage" should "create a nice string" in new BasicFilmResponse {
    val expected: String =
    """ |**The Wicker Man (1973)**
        |**Run time**  : 120
        |**Director**  : Robin Hardy
        |**Starring**  : Christopher Lee
        |**Genre**     : Horror
        |**Plot**      : A bit of craic
        |**Ratings**   :
        |imbd - 10
        |https://upload.wikimedia.org/wikipedia/en/1/11/The_Wicker_Man_%281973_film%29_UK_poster.jpg""".stripMargin
    val actual: String = basicInput.asMessage
    actual must be(expected)
  }

  "can parse the desired json using the shared trait" should "be parsable" in new ActualJson {
    import FilmResponseImplicits._

    val parsed: OmdbapiResponses = actualJson.parseJson.convertTo[OmdbapiResponses]
    val v: FilmResponse = parsed.asInstanceOf[FilmResponse]
    v.title mustBe "Natural Born Killers"
    v.actors mustBe "Woody Harrelson, Juliette Lewis, Tom Sizemore, Rodney Dangerfield"
    v.director mustBe "Oliver Stone"
  }

  "can handle errors" should "be parsable as error object" in new ErrorResponse {
    import FilmResponseImplicits._

    val parsed: OmdbapiResponses = err.parseJson.convertTo[OmdbapiResponses]
    val asErr: FilmErrorResponse = parsed.asInstanceOf[FilmErrorResponse]
    asErr.response mustBe "False"
  }
}

trait ActualJson {
  val actualJson: String =
    """
      |{
      |  "Title": "Natural Born Killers",
      |  "Year": "1994",
      |  "Rated": "R",
      |  "Released": "26 Aug 1994",
      |  "Runtime": "118 min",
      |  "Genre": "Action, Crime, Drama",
      |  "Director": "Oliver Stone",
      |  "Writer": "Quentin Tarantino (story), David Veloz (screenplay), Richard Rutowski (screenplay), Oliver Stone (screenplay)",
      |  "Actors": "Woody Harrelson, Juliette Lewis, Tom Sizemore, Rodney Dangerfield",
      |  "Plot": "Two victims of traumatized childhoods become lovers and psychopathic serial murderers irresponsibly glorified by the mass media.",
      |  "Language": "English, Navajo, Japanese",
      |  "Country": "USA",
      |  "Awards": "Nominated for 1 Golden Globe. Another 5 wins & 10 nominations.",
      |  "Poster": "https://m.media-amazon.com/images/M/MV5BMTI2NTU2Nzc0MV5BMl5BanBnXkFtZTcwMzY1OTM2MQ@@._V1_SX300.jpg",
      |  "Ratings": [
      |    {
      |      "Source": "Internet Movie Database",
      |      "Value": "7.3/10"
      |    },
      |    {
      |      "Source": "Rotten Tomatoes",
      |      "Value": "48%"
      |    },
      |    {
      |      "Source": "Metacritic",
      |      "Value": "74/100"
      |    }
      |  ],
      |  "Metascore": "74",
      |  "imdbRating": "7.3",
      |  "imdbVotes": "214,471",
      |  "imdbID": "tt0110632",
      |  "Type": "movie",
      |  "DVD": "N/A",
      |  "BoxOffice": "N/A",
      |  "Production": "J D Productions, Warner Brothers, New Regency Pictures, Ixtlan Corporation",
      |  "Website": "N/A",
      |  "Response": "True"
      |}
      |""".stripMargin
}

trait BasicFilmResponse {
  val basicInput: FilmResponse = FilmResponse(
    title     = "The Wicker Man",
    year      = "1973",
    runtime   = "120",
    genre     = "Horror",
    director  = "Robin Hardy",
    actors    = "Christopher Lee",
    poster    = "https://upload.wikimedia.org/wikipedia/en/1/11/The_Wicker_Man_%281973_film%29_UK_poster.jpg",
    plot      = "A bit of craic",
    ratings   = List(
        Rating("imbd", "10"),
        //Rating("rt", "10")
      )
    )
}

trait ErrorResponse {
  val err = """{"Response":"False","Error":"Movie not found!"}"""
}