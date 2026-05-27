package models

import ackcord.APIMessage.{MessageReactionAdd, MessageReactionRemove}
import ackcord.data.{Message, User}
import slick.jdbc.SQLiteProfile.api._
import slick.lifted.ProvenShape

case class CombinedView(
  suggestion: Suggestion,
  meta: SuggestionMetadata,
  votes: SuggestionVote
)

object Suggestion {
  def fromMessage(message: Message): Suggestion =
    Suggestion(
      message.id.toUnsignedLong,
      message.authorUsername,
      message.content,
      message.timestamp.toEpochSecond
    )
}

case class Suggestion(messageId: Long, userName: String, filmName: String, timestamp: Long)

class Suggestions(tag: Tag) extends Table[Suggestion](tag, "suggestions") {
  def messageId = column[Long]("message_id", O.PrimaryKey)
  def userName = column[String]("user_name")
  def filmName = column[String]("film_name")
  def timestamp = column[Long]("timestamp")
  def * : ProvenShape[Suggestion] =
    (messageId, userName, filmName, timestamp) <> ((Suggestion.apply _).tupled, Suggestion.unapply)
}

object SuggestionVote {
  // It's annoying these two message types don't share a trait, because they're basically identical
  def fromReaction(message: MessageReactionAdd): SuggestionVote = SuggestionVote(
    0L,
    message.messageId.asInstanceOf[Long],
    message.emoji.name.getOrElse("?"),
    optionUserToString(message.user)
  )

  def fromReaction(message: MessageReactionRemove): SuggestionVote = SuggestionVote (
    0L,
    message.messageId.asInstanceOf[Long],
    message.emoji.name.getOrElse("?"),
    optionUserToString(message.user)
  )

  private def optionUserToString(u: Option[User]): String =
    u match {
      case Some(x) => x.username
      case None => "unknown"
    }
}

case class SuggestionVote(id: Long, messageId: Long, emote: String, author: String)

// TODO: Refactor this to use foreign keys
class SuggestionVotes(tag: Tag) extends Table[SuggestionVote](tag, "votes") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def messageId = column[Long]("message_id")
  def emote = column[String]("emote")
  def author = column[String]("author")

  def * : ProvenShape[SuggestionVote] =
    (id, messageId, emote, author) <> ((SuggestionVote.apply _).tupled, SuggestionVote.unapply)
}

case class SuggestionMetadata(messageId: Long, poster: Array[Byte], title: String, runtime: Int = 0)

class SuggestionMetadataT(tag: Tag) extends Table[SuggestionMetadata](tag, "suggestion_metadata") {
  def messageId = column[Long]("message_id", O.PrimaryKey)
  def poster = column[Array[Byte]]("poster")
  def title = column[String]("title")
  def runtime = column[Int]("runtime")
  def * : ProvenShape[SuggestionMetadata] =
    (messageId, poster, title, runtime) <> ((SuggestionMetadata.apply _).tupled, SuggestionMetadata.unapply)
}

case class ServerMetadata(key: String, value: String)

class ServerMetadataT(tag: Tag) extends Table[ServerMetadata](tag, "server_metadata") {
  def key = column[String]("key", O.PrimaryKey)
  def value = column[String]("value")
  def * : ProvenShape[ServerMetadata] =
    (key, value) <> ((ServerMetadata.apply _).tupled, ServerMetadata.unapply)
}

