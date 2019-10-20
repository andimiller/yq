package net.andimiller.yq.commands

import java.nio.file.Path

import com.monovore.decline.Opts
import io.circe.Json
import net.andimiller.yq.formats.Format
import cats._
import cats.effect.{Blocker, Sync}
import cats.implicits._
import higherkindness.droste.Algebra
import higherkindness.droste.scheme.cata
import io.circe.rs.JsonF
import net.andimiller.yq.Utils.FileLoader

object SortKeys {
  import net.andimiller.yq.droste._

  val keySorter: Algebra[JsonF, Json] = Algebra[JsonF, Json] {
    case JsonF.JObjectF(fields) =>
      Json.obj(fields.sortBy(_._1): _*)
    case jf =>
      JsonF.foldJson(jf)
  }

  def apply[F[_]: Sync](loader: FileLoader[F], blocker: Blocker): Command[F] = new Command[F] {
    val parser: Opts[Format => F[Json]] =
      Opts.subcommand("sort", "sort the keys in a file", true)(Opts.argument[Path]("file")).map { p =>
        { f: Format =>
          loader.loadFile(p, blocker).map(cata(keySorter).apply).flatTap { j =>
            Sync[F].delay {
              println(f.print(j))
            }
          }

        }
      }

  }
}
