import functions.Responders.{FilmCountResponder, FilmSuggestionResponder, Japes, PosterResponder, Respond}
import repositories._
import services.{FetchFilmInfo, FilmVoteHelpers, PosterGenerator, SuggestionHandler}
import ackcord._
import ackcord.data.GuildChannel
import akka.actor.ActorSystem
import config.{Bb3Config, ConfigInit}
import models.{Constants, ServerMetadata}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  final val clientName = "bb3tbbbb"

  implicit val as: ActorSystem = ActorSystem("bb3")

  val config: Bb3Config = ConfigInit.config match {
    case Right(value) => value
    case Left(value) => sys.error(s"Failed to parse config: ${value.prettyPrint(4)}")
  }

  val clientSettings = ClientSettings(config.discordToken)

  val client: DiscordClient = Await.result(clientSettings.createClient(), 5.seconds)
  import client.executionContext

  client.login()

  val suggestionsRepo: SuggestionsRepoSlick = new SuggestionsRepoSlick(config)
  val voteRepo: VoteRepoSlick = new VoteRepoSlick(config)
  val metadataRepo: SuggestionMetadataRepo = new SuggestionMetadataRepoSlick(config)

  val serverMetadataRepo: ServerMetadataRepo = new ServerMetadataRepoSlick(config)

  val fetchFilmInfo = new FetchFilmInfo(config.omdbapiToken)
  val filmVoteHelpers = new FilmVoteHelpers(suggestionsRepo, voteRepo)
  val suggestionHandler = new SuggestionHandler(suggestionsRepo, metadataRepo, fetchFilmInfo)
  val posterGenerator = new PosterGenerator(metadataRepo, serverMetadataRepo)


  val messageResponder = new Respond(
    Seq(
      new FilmSuggestionResponder(suggestionHandler, clientName),
      new FilmCountResponder(clientName, filmVoteHelpers),
      new PosterResponder(clientName, filmVoteHelpers, posterGenerator),
      new Japes
    ),
    client
  )

  client.onEventAsync {
    implicit cacheSnapshot: CacheSnapshot => {
      case APIMessage.Ready(_) =>
        println("Connected")
        OptFuture.fromOption(None)

      case m: APIMessage.MessageCreate =>
        println("Parsing message")
        messageResponder(m)

      case r: APIMessage.MessageReactionAdd =>
        println("Parsing reaction add")
        OptFuture.fromFuture(
          filmVoteHelpers.addVote(r)
        )

      case r: APIMessage.MessageReactionRemove =>
        println("Parsing reaction remove")
        OptFuture.fromFuture(
          filmVoteHelpers.delVote(r)
        )

      case APIMessage.MessageDelete(messageId, _, _, _) =>
        // TODO: This will always try to delete a suggestion. Check the channel id first to save IO
        OptFuture.fromFuture(
          filmVoteHelpers.delSuggestion(messageId)
        )

      case APIMessage.ChannelUpdate(_, channel, _) =>
        if (channel.id.asInstanceOf[Long] == Constants.Channels.general) {
          val asGuild = channel.id.asChannelId[GuildChannel].resolve
          if (asGuild.isDefined)
            OptFuture.fromFuture(
              serverMetadataRepo.update(
                ServerMetadata("general_channel_name", asGuild.get.name)
              ).map(_ =>())
            )
          else OptFuture.fromOption(None)
        }
        else OptFuture.fromOption(None)

      case x =>
        println(s"Hit case ${x.getClass.getName}")
        OptFuture.fromOption(None)
    }
  }
}