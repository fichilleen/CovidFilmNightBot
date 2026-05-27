package functions.Responders

import functions.Util
import services.FilmVoteHelpers
import ackcord.APIMessage.MessageCreate
import ackcord.CacheSnapshot
import ackcord.data.Message
import ackcord.requests.{CreateMessage, CreateMessageData}
import models.SuggestionWithVotes

import scala.collection.immutable.HashMap
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FilmCountResponder(clientName: String, filmVoteHelpers: FilmVoteHelpers)(implicit ec: ExecutionContext) extends Responder {
  override def debug: Boolean = true
  private val triggers = Seq("top!", "count!", "revcount!", "order!")

  private val intToWordLookup = HashMap[Int, String](
    1 -> ":one:",
    2 -> ":two:",
    3 -> ":three:",
    4 -> ":four:",
    5 -> ":five:",
    6 -> ":six:",
  )

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
    filmVoteHelpers.getWeekCount.map { suggestionsWithVotes =>
      CreateMessage(
        message.channelId, CreateMessageData(
          getResult(suggestionsWithVotes, message.content)
        )
      )
    }

  private def getResult(sug: Seq[SuggestionWithVotes], trigger: String): String = {
    val result = trigger match {
      case "count!" => asCount(sug)
      case "revcount!" => asCount(sug, reverse = true)
      case "top!" => asTop(sug)
      case "order!" => asOrder(sug)
    }
    if (result.isEmpty)
      "No suggestions yet"
    else
      result
  }

  private def asCount(sug: Seq[SuggestionWithVotes], reverse: Boolean = false): String = {
    val suggestions =
      if (reverse) sug.sortBy(_.score).reverse
      else sug.sortBy(_.score)
    val long = suggestions.map(_.asMessageWithLinks).mkString("\n")
    if (long.length >= 2000)
      suggestions.map(_.asMessage).mkString("\n")
    else long
  }

  private def asTop(sug: Seq[SuggestionWithVotes]): String =
    sug.sortBy(_.score)
      .takeRight(5)
      .map(_.asMessageWithLinks)
      .mkString("\n")

  private def asOrder(sug: Seq[SuggestionWithVotes]): String = {
    def datePattern(localDateTime: LocalDateTime): String =
      localDateTime.format(DateTimeFormatter.ofPattern("kk:mm"))

    def addTimes(films: Seq[SuggestionWithVotes]): String = {
      films.scanLeft(Util.nextFilmStartTime) { case (a, b) =>
        a.plusMinutes(b.suggestionMetadata.runtime.toLong)
      }
        .zip(films)
        .map{case (t, s) =>
          s"**${datePattern(t)}** - ${s.suggestionMetadata.title}"
        }
        .mkString("\n")
    }

    sug
      .sortBy(_.score)
      .takeRight(3)
      .permutations
      .map(addTimes)
      .zipWithIndex
      .map{ case (x, i) =>
        val emote = intToWordLookup(i+1)
        s"$emote\n$x"
      }
      .mkString("\n=====\n")
  }
}
