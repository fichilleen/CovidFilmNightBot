package functions.Responders

import functions.Util.textChannelToGuildChannel
import services.SuggestionHandler
import ackcord.APIMessage.MessageCreate
import ackcord.CacheSnapshot
import ackcord.data.{GuildChannel, Message}
import ackcord.requests.CreateMessage

import scala.concurrent.Future

class FilmSuggestionResponder(suggestionHandler: SuggestionHandler, clientName: String) extends Responder {
  override def debug: Boolean = true

  private def isValidSuggestion(channel: GuildChannel, message: Message): Boolean =
    channel.name == "saturday-film-votes" &&
      message.authorUsername != clientName &&
      message.content != "count!"

  override def matches(message: MessageCreate)(implicit c: CacheSnapshot): Boolean =
    message match {
      case MessageCreate(guild, message, _) if guild.isDefined =>
        textChannelToGuildChannel(message.channelId) match {
          case Some(c: GuildChannel) if isValidSuggestion(c, message) => true
          case _ => false
        }
      case _ => false
    }

  override def apply(message: Message)(implicit c: CacheSnapshot): Future[CreateMessage] =
    suggestionHandler.takeSuggestion(message)

}
