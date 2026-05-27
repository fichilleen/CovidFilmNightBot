package models
import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

import scala.reflect.ClassTag


trait PascalCaseJsonProtocol extends DefaultJsonProtocol {
  override protected def extractFieldNames(classTag: ClassTag[_]): Array[String] =
    super.extractFieldNames(classTag).map(pascalCase)

  def pascalCase(name: String): String = {
    val x = name.head
    x.toString.toUpperCase ++ name.tail
  }
}

object PascalCaseJsonProtocol extends PascalCaseJsonProtocol

trait OmdbapiResponses
case class FilmErrorResponse(response: String, error: String) extends OmdbapiResponses

case class FilmResponse(
  title: String,
  year: String,
  runtime: String,
  genre: String,
  poster: String,
  plot: String,
  director: String,
  actors: String,
  ratings: List[Rating]
) extends OmdbapiResponses
{
  def asMessage: String = {
    val critics = ratings.map(s => s"${s.source} - ${s.value}").mkString("\n")
    val biggerPoster = poster.replace("300.jpg", "1200.jpg")
    s"""|**$title ($year)**
       |**Run time**  : $runtime
       |**Director**  : $director
       |**Starring**  : $actors
       |**Genre**     : $genre
       |**Plot**      : $plot
       |**Ratings**   :
       |$critics
       |$biggerPoster""".stripMargin
  }
}

case class Rating(source: String, value: String)
case class AllRatings(ratings: List[Rating])

object FilmResponseImplicits extends PascalCaseJsonProtocol {

  implicit val ratingFormat: RootJsonFormat[Rating] = jsonFormat2(Rating)

  implicit val allRatingsFormat: RootJsonFormat[AllRatings] = jsonFormat1(AllRatings)
  implicit val filmResponseFormat: RootJsonFormat[FilmResponse] = jsonFormat9(FilmResponse)
  implicit val filmErrorFormat: RootJsonFormat[FilmErrorResponse] = jsonFormat2(FilmErrorResponse)

  implicit object OmdbapiResponseFormat extends RootJsonFormat[OmdbapiResponses] with PascalCaseJsonProtocol {
    override def read(json: JsValue): OmdbapiResponses =
      if (json.asJsObject.getFields("Error").nonEmpty)
        json.convertTo[FilmErrorResponse]
      else
        json.convertTo[FilmResponse]

    override def write(obj: OmdbapiResponses): JsValue = obj match {
      case x: FilmErrorResponse => filmErrorFormat.write(x)
      case y: FilmResponse => filmResponseFormat.write(y)
    }
  }
}


