package net.andimiller.yq.formats

import io.circe.{Json, Printer}

trait Format {
  def parse(s: String): Either[Throwable, Json]
  def print(j: Json): String
}

object Yaml extends Format {
  import io.circe.yaml.Printer
  override def print(j: Json): String                    = Printer(preserveOrder = true).pretty(j)
  override def parse(s: String): Either[Throwable, Json] = io.circe.yaml.parser.parse(s)
}

class JSON(pretty: Boolean = true) extends Format {
  val printer: Printer                                   = if (pretty) io.circe.Printer.spaces2 else io.circe.Printer.noSpaces
  override def print(j: Json): String                    = printer.print(j)
  override def parse(s: String): Either[Throwable, Json] = io.circe.parser.parse(s)
}
