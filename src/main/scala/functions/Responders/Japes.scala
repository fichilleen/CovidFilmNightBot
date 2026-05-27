package functions.Responders

import ackcord.APIMessage.MessageCreate
import ackcord.CacheSnapshot
import ackcord.data.Message
import ackcord.requests.{CreateMessage, CreateMessageData}

import scala.concurrent.Future
import scala.util.Random

class Japes extends Responder {
  def someweather: String = {
    val heat = Random.between(-50, 50)
    val wind = Random.between(0, 300)
    val desc = getRandomElement(Seq("strong", "light", "mild", "moderate"))
    val behv = getRandomElement(Seq("snow", "rain", "blizzard", "sunshine", "overcast"))

    s"Weather in Belfast: ${heat}c, $desc $behv with winds gusting up to ${wind}mph"
  }

  private def getRandomElement[A](seq: Seq[A]): A =
    seq(Random.nextInt(seq.length))

  override def apply(message: Message)(implicit c: CacheSnapshot): Future[CreateMessage] =
    Future.successful(
      CreateMessage(
        message.channelId, CreateMessageData(
          someweather
        )
      )
    )

  override def matches(message: MessageCreate)(implicit c: CacheSnapshot): Boolean =
    message match {
      case MessageCreate(_, message, _) =>
        if (message.content.contains("some weather now boys!")) true
        else false
      case _ => false
    }
}