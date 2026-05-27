package services

import repositories.{SuggestionsRepo, VoteRepo}
import ackcord.APIMessage.{MessageReactionAdd, MessageReactionRemove}
import ackcord.data.MessageId
import models.{SuggestionVote, SuggestionWithVotes}

import scala.concurrent.{ExecutionContext, Future}

class FilmVoteHelpers(suggestionsRepo: SuggestionsRepo, voteRepo: VoteRepo)(implicit ec: ExecutionContext){
  def getWeekCount: Future[Seq[SuggestionWithVotes]] =
    suggestionsRepo.getAllForWeek

  def addVote(msg: MessageReactionAdd): Future[Unit] =
    voteRepo
      .put(SuggestionVote.fromReaction(msg))
      .map(_ => ())

  def delVote(msg: MessageReactionRemove): Future[Unit] =
    voteRepo
      .del(SuggestionVote.fromReaction(msg))
      .map(_ => ())

  def delSuggestion(messageId: MessageId): Future[Unit] =
    suggestionsRepo.del(messageId).map(_ => ())
}
