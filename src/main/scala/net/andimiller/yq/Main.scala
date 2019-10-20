package net.andimiller.yq

import cats._
import cats.implicits._
import cats.effect._
import com.monovore.decline._
import net.andimiller.yq.Utils.FileLoader
import net.andimiller.yq.commands.{Resolver, SortKeys}

object Main extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO].use { b =>
      Command("yq", "yaml query tool, pronounced yuck") {
        Resolver[IO](FileLoader[IO](formats.Yaml), b).parser.orElse(
          SortKeys[IO](FileLoader[IO](formats.Yaml), b).parser
        )
      }.parse(args) match {
        case Left(h) =>
          IO { println(h.toString) }.as(ExitCode.Error)
        case Right(f) =>
          f(formats.Yaml).as(ExitCode.Success)
      }

    }

}
