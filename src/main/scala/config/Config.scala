package config

import pureconfig.ConfigReader.Result
import pureconfig._
import pureconfig.generic._
import pureconfig.generic.auto._


case class Bb3Config(
  discordToken: String,
  omdbapiToken: String,
  bbSqliteFile: String,
)

object ConfigInit {
  val config: Result[Bb3Config] = ConfigSource.default.load[Bb3Config]
}