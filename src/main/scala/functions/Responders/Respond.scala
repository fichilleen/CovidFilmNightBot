package functions.Responders

import ackcord.APIMessage.MessageCreate
import ackcord.{CacheSnapshot, DiscordClient, OptFuture}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class Respond(responders: Seq[Responder], client: DiscordClient)(implicit  ec: ExecutionContext)
{
  def apply(messageCreate: MessageCreate)(implicit cacheSnapShot: CacheSnapshot): OptFuture[Unit] = {
    val validResponses = responders.filter(_.matches(messageCreate))

    if(validResponses.nonEmpty) {
      val responder = validResponses.head

      responder.apply(messageCreate.message).onComplete {
        case Success(f) =>
          client.requestsHelper.run(f).map{ x =>
            if (responder.debug) println(x)
            else ()
          }
        case Failure(e) =>
          println(s"Future failed in ${responder.getClass.getName}")
          print(s"Exception: ${e.getStackTrace.mkString("Array(", ", ", ")")}")
          OptFuture.fromFuture(Future.successful(None))
      }
    }
    OptFuture.fromOption(None)
  }
}
