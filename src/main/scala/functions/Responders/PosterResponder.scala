package functions.Responders

import services.{FilmVoteHelpers, PosterGenerator}
import ackcord.APIMessage.MessageCreate
import ackcord.CacheSnapshot
import ackcord.data.Message
import ackcord.requests.{CreateMessage, CreateMessageData, CreateMessageFile}
import akka.http.scaladsl.model.MediaTypes
import akka.util.ByteString
import models.SuggestionWithVotes

import scala.concurrent.{ExecutionContext, Future}

class PosterResponder(clientName: String, filmVoteHelpers: FilmVoteHelpers, posterGenerator: PosterGenerator)(implicit ec: ExecutionContext) extends Responder {
  override def debug: Boolean = true
  private val triggers = Seq("poster!")

  private def isValidSuggestion(message: Message): Boolean =
    message.authorUsername != clientName && triggers.contains(message.content)

  override def matches(message: MessageCreate)(implicit c: CacheSnapshot): Boolean =
    message match {
      case MessageCreate(_, message, _) =>
        if (isValidSuggestion(message)) true
        else false
      case _ => false
    }

  override def apply(message: Message)(implicit c: CacheSnapshot): Future[CreateMessage] =
    filmVoteHelpers.getWeekCount.flatMap { suggestionsWithVotes =>
      getPoster(suggestionsWithVotes).map( poster =>
        if(poster.isDefined) {
          CreateMessage(
            message.channelId, CreateMessageData(
              files = Seq(
                CreateMessageFile.ByteFile(MediaTypes.`image/png`, poster.get, "poster.png")
              )
            )
          )
        }
        else CreateMessage(message.channelId, CreateMessageData("Not enough metadata for a poster"))
      )
    }

  private def getPoster(sug: Seq[SuggestionWithVotes], n: Int = 3): Future[Option[ByteString]] =
    posterGenerator.generatePosterData(
      sug
        .sortBy(_.score)
        .takeRight(n)
        .map(_.suggestion.messageId)
    ).map { p =>
      if (p.isDefined)
        Some(ByteString(p.get))
      else None
    }
}
