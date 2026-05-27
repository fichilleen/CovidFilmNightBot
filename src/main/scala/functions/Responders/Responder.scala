package functions.Responders

import ackcord.APIMessage.MessageCreate
import ackcord.CacheSnapshot
import ackcord.data.Message
import ackcord.requests.CreateMessage

import scala.concurrent.Future

trait Responder {
  def apply(message: Message)(implicit c: CacheSnapshot): Future[CreateMessage]
  def debug: Boolean = false
  def matches(messageCreate: MessageCreate)(implicit c: CacheSnapshot): Boolean
}
