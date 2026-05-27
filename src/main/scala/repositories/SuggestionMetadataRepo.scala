package repositories

import config.Bb3Config
import models.{SuggestionMetadata, SuggestionMetadataT}
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Future

trait SuggestionMetadataRepo {
  def put(suggestionMetadata: SuggestionMetadata): Future[Int]
  def del(messageId: Long): Future[Int]
  def get(messageId: Long): Future[Option[SuggestionMetadata]]
}

class SuggestionMetadataRepoSlick(config: Bb3Config) extends SuggestionMetadataRepo {
  val db = Database.forURL(s"jdbc:sqlite:${config.bbSqliteFile}", driver = "org.sqlite.JDBC")
  val tableQuery = TableQuery[SuggestionMetadataT]

  def put(suggestionMetadata: SuggestionMetadata): Future[Int] =
    db.run(
      tableQuery += suggestionMetadata
    )

  def del(messageId: Long): Future[Int] =
    db.run(
      tableQuery
        .filter(_.messageId === messageId)
        .delete
    )

  def get(messageId: Long): Future[Option[SuggestionMetadata]] =
    db.run(
      tableQuery.filter(
        _.messageId === messageId
      ).result.headOption
    )
}
