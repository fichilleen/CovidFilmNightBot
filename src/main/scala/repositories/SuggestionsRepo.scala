package repositories

import functions.Util.lastFilmStartTime
import ackcord.data.MessageId
import config.Bb3Config
import models._
import slick.jdbc.SQLiteProfile.api._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait SuggestionsRepo {
  def put(filmSuggestion: Suggestion): Future[Int]
  def del(messageId: MessageId): Future[Int]
  def get(messageId: Long): Future[Option[Suggestion]]
  def getWeek: Future[Seq[Suggestion]]
  def getAllForWeek: Future[Seq[SuggestionWithVotes]]
}

class SuggestionsRepoSlick(config: Bb3Config)(implicit val ec: ExecutionContext) extends SuggestionsRepo {
  val db = Database.forURL(s"jdbc:sqlite:${config.bbSqliteFile}", driver = "org.sqlite.JDBC")

  val suggestionTable = TableQuery[Suggestions]
  val metadataTable = TableQuery[SuggestionMetadataT]
  val voteTable = TableQuery[SuggestionVotes]

  override def put(filmSuggestion: Suggestion): Future[Int] =
    db.run(
      suggestionTable += filmSuggestion
    )

  override def del(messageId: MessageId): Future[Int] =
    db.run(
      suggestionTable.filter(
        _.messageId === messageId.asInstanceOf[Long]
      ).delete
    )

  override def get(messageId: Long): Future[Option[Suggestion]] =
    db.run(
      suggestionTable.filter(
        _.messageId === messageId
      ).result.headOption
    )

  override def getWeek: Future[Seq[Suggestion]] =
    db.run(
      suggestionTable.filter(
        _.timestamp > lastFilmStartTime
      ).result
    )

  override def getAllForWeek: Future[Seq[SuggestionWithVotes]] = {
    val x = for {
      (v, s) <- voteTable.joinLeft(
        suggestionTable
          .filter(_.timestamp > lastFilmStartTime)
          .join(metadataTable).on(_.messageId === _.messageId)
      ).on(_.messageId === _._1.messageId)
    } yield (v, s)

    db.run(x.result).map(combinedView)
  }

  private def combinedView(input: Seq[(SuggestionVote, Option[(Suggestion, SuggestionMetadata)])]): Seq[SuggestionWithVotes] = {
    val collected = mutable.HashMap[Suggestion, (SuggestionMetadata, Seq[SuggestionVote])]()

    input.foreach { case (votes, key) =>
      if (key.isDefined) {
        val k = key.get._1
        val m = key.get._2
        println(s"added $k")
        collected.get(k) match {
          case Some((meta, v)) => collected(k) = (meta, v ++ Seq(votes) )
          case None => collected(k) = (m, Seq(votes))
        }
      }
    }

    println(s"collection: $collected")
    collected.map{ case (suggestion, (meta, votes)) =>
      SuggestionWithVotes(
        suggestion,
        votes,
        meta
      )
    }.toSeq
  }
}
