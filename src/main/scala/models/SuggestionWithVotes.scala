package models

case class SuggestionWithVotes(suggestion: Suggestion, votes: Seq[SuggestionVote], suggestionMetadata: SuggestionMetadata){
  final private val negativeVotes = Seq(
    "\uD83D\uDC4E", // Thumbs down
    "👎"
  )

  private def emojiOutput: String =
    votesLimitedByUser.sortBy(_.emote).map(_.emote).mkString("")

  private def votesLimitedByUser: Seq[SuggestionVote] =
    this.votes.distinctBy(_.author)

  def score: Int = {
    val (neg, pos) = this.votesLimitedByUser.partition(s => negativeVotes.contains(s.emote))
    pos.length - neg.length
  }

  def asMessage: String =
    s"$emojiOutput ${suggestionMetadata.title}"

  def asMessageWithLinks: String =
    s"$emojiOutput ${suggestionMetadata.title}\nhttps://discord.com/channels/${Constants.Channels.votes}/698484393282371604/${suggestion.messageId}"
}
