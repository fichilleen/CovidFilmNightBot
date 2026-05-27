package functions

import ackcord.CacheSnapshot
import ackcord.data.{GuildChannel, TextChannelId}

import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.{DayOfWeek, LocalDate, LocalDateTime, ZoneOffset}

object Util {
  def textChannelToGuildChannel(textChannelId: TextChannelId)(implicit c: CacheSnapshot): Option[GuildChannel] =
    textChannelId.asChannelId[GuildChannel].resolve

  def lastFilmStartTime: Long = {
    val now = LocalDateTime.now()
    val dayOffset =
      if (now.getDayOfWeek == DayOfWeek.SATURDAY && now.getHour < 20)
        TemporalAdjusters.previous(DayOfWeek.SATURDAY)
      else
        TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)

    LocalDate
      .now()
      .`with`(dayOffset)
      .atTime(20, 0)
      .toEpochSecond(ZoneOffset.UTC)
  }

  def nextFilmStartTime: LocalDateTime =
    LocalDate
      .now()
      .`with`(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
      .atTime(20, 0)

  def tonightFormat(localDateTime: LocalDateTime): String =
    localDateTime.format(DateTimeFormatter.ofPattern("EEEE, dd-MM-yyyy 'at' kk:mm"))
}
