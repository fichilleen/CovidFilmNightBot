package services

import functions.Util
import repositories.{ServerMetadataRepo, SuggestionMetadataRepo}
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.canvas.GraphicsContext
import com.sksamuel.scrimage.canvas.drawables.Text
import com.sksamuel.scrimage.color.{HSVColor, RGBColor}
import com.sksamuel.scrimage.implicits._
import com.sksamuel.scrimage.nio.PngWriter
import models.ServerMetadata

import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class PosterGenerator(metadataRepo: SuggestionMetadataRepo, serverMetadataRepo: ServerMetadataRepo)(implicit ec: ExecutionContext) {

  private val discordBgColour: HSVColor =
    new RGBColor(54, 57, 63).toHSV

  private val newImage: ImmutableImage =
    ImmutableImage.create(900, 600).fill(discordBgColour)

  def generatePosterData(filmIds: Seq[Long]): Future[Option[Array[Byte]]] =
    fetchImages(filmIds).map { images =>
      getChannelName.map( s =>
        if (images.length == filmIds.length) Some(mkPoster(images, s))
        else None
      )
    }.flatten

  private def overlayText(text: String, scale: Int): ImmutableImage =
    ImmutableImage.create(900, 20)
      .fill(discordBgColour)
      .draw(new Text(text, 10, 10, GraphicsContext.identity()))
      .scale(scale.toDouble)

  private def timeString: String =
    Util.nextFilmStartTime.format(DateTimeFormatter.ofPattern("EEEE, dd-MM-yyyy 'at' kk:mm"))

  private def mkPoster(images: Seq[ImmutableImage], channelName: String): Array[Byte] = {
    var i = 0 // May god forgive me
    val joined = images.fold(newImage){ (a, b) =>
      val o = a.overlay(b.scaleToWidth(300), i, 100)
      i += 300
      o
    }
    joined
      .overlay(overlayText("Film night presents", 3), 0, 15)
      .overlay(overlayText(s"In association with $channelName", 2), 35, 55)
      .overlay(overlayText(timeString, 2), 0, 570)
      .bytes(PngWriter.MaxCompression)
  }

  private def getChannelName: Future[String] =
    serverMetadataRepo
      .get("general_channel_name")
      .map(
        _.getOrElse(ServerMetadata("general_channel_name", "film-voting-council"))
        .value
        .replace("-", " ")
        .replace("-", " ")
        .split(" ").map(_.capitalize).mkString(" ")
      )

  private def fetchImages(filmIds: Seq[Long]): Future[Seq[ImmutableImage]] = {
    Future.sequence(
      filmIds.map{ id =>
        metadataRepo.get(id).map{ md =>
          md.map( x =>
            ImmutableImage
              .loader()
              .fromBytes(x.poster)
          )
        }
      }
    ).map(_.flatten)
  }
}
