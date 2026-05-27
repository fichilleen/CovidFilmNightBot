package services

import repositories.{SuggestionMetadataRepo, SuggestionsRepo}
import ackcord.data.Message
import ackcord.requests.{CreateMessage, CreateMessageData}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import models.{FilmErrorResponse, FilmResponse, Suggestion, SuggestionMetadata}

import scala.concurrent.{ExecutionContext, Future}

class SuggestionHandler(suggestionsRepo: SuggestionsRepo, metadataRepo: SuggestionMetadataRepo, fetchFilmInfo: FetchFilmInfo)
                       (implicit ec: ExecutionContext, as: ActorSystem, mat: Materializer)
{
  private val maxSuggestionsPerUserPerWeek = 3

  // TODO: We should probably handle deletes as well

  def takeSuggestion(messsage: Message): Future[CreateMessage] = {
    tooManySuggestions(messsage).flatMap( tooMany =>
      if (tooMany)
        Future.successful(
          CreateMessage(messsage.channelId, CreateMessageData(s"You've already suggested enough this week ${messsage.authorUsername}"))
        )
      else {
        println(s"Got message: ${messsage.content}")
        fetchFilmInfo.fetchFilmInfo(messsage.content).map {
          case film: FilmResponse =>
            saveSuggestion(film, messsage)
            CreateMessage(messsage.channelId, CreateMessageData(content = film.asMessage))
          case err: FilmErrorResponse =>
            CreateMessage(messsage.channelId, CreateMessageData(content = err.error))
        }
      }
    )
  }

  private def saveSuggestion(filmResponse: FilmResponse, message: Message): Future[Int] = {
    val suggestionCommit = suggestionsRepo.put(Suggestion.fromMessage(message))
    val runtime = filmResponse.runtime.split(" ").head.toIntOption.getOrElse(0)

    // We don't bother mapping this one because it'll be done when it's done
    // it's mostly acceptable for it to fail (at least while posters are experimental)
    downloadPoster(filmResponse.poster).map { poster =>
      metadataRepo.put(
        SuggestionMetadata(
          message.id.asInstanceOf[Long],
          poster,
          s"${filmResponse.title} (${filmResponse.year})",
          runtime
        )
      )
    }
    suggestionCommit
  }

  private def downloadPoster(posterUrl: String): Future[Array[Byte]] =
    Http()
      .singleRequest(Get(posterUrl))
      .flatMap(Unmarshal(_).to[Array[Byte]])

  private def tooManySuggestions(message: Message): Future[Boolean] =
    suggestionsRepo.getWeek.map(
      s => s.map(_.userName).count( _ == message.authorUsername) >= maxSuggestionsPerUserPerWeek
    )
}
