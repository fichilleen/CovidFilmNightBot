package repositories

import config.Bb3Config
import models.{ServerMetadata, ServerMetadataT, SuggestionMetadata, SuggestionMetadataT}
import slick.jdbc.SQLiteProfile.api._

import scala.concurrent.Future

trait ServerMetadataRepo {
  def get(key: String): Future[Option[ServerMetadata]]
  def update(serverMetadata: ServerMetadata): Future[Int]
}

class ServerMetadataRepoSlick(config: Bb3Config) extends ServerMetadataRepo {
  val db = Database.forURL(s"jdbc:sqlite:${config.bbSqliteFile}", driver = "org.sqlite.JDBC")
  val tableQuery = TableQuery[ServerMetadataT]

  def get(key: String): Future[Option[ServerMetadata]] =
    db.run(
      tableQuery
        .filter(_.key === key)
        .result.headOption
    )

  def update(serverMetadata: ServerMetadata): Future[Int] =
    db.run(
      tableQuery
        .filter(_.key === serverMetadata.key)
        .map(x => (x.key, x.value))
        .update((serverMetadata.key, serverMetadata.value))
    )
}
