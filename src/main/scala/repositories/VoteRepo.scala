package repositories

import config.Bb3Config
import models.{SuggestionVote, SuggestionVotes}
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Future

trait VoteRepo {
  def put(suggestionVote: SuggestionVote): Future[Int]
  def del(suggestionVote: SuggestionVote): Future[Int]
  def get(messageId: Long): Future[Seq[SuggestionVote]]
}

class VoteRepoSlick(config: Bb3Config) extends VoteRepo {
  val db = Database.forURL(s"jdbc:sqlite:${config.bbSqliteFile}", driver = "org.sqlite.JDBC")
  val tableQuery = TableQuery[SuggestionVotes]

  def put(suggestionVote: SuggestionVote): Future[Int] =
    db.run(
      tableQuery += suggestionVote
    )

  def del(suggestionVote: SuggestionVote): Future[Int] =
    db.run(
      tableQuery
        .filter(_.messageId === suggestionVote.messageId)
        .filter(_.emote === suggestionVote.emote)
        .filter(_.author === suggestionVote.author)
        .delete
    )

  def get(messageId: Long): Future[Seq[SuggestionVote]] =
    db.run(
      tableQuery.filter(
        _.messageId === messageId
      ).result
    )
}
