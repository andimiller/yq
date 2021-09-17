package net.andimiller.yq.commands

import cats._
import cats.effect._
import cats.implicits._
import com.monovore.decline._
import io.circe.Json
import net.andimiller.yq.Utils.FileLoader
import net.andimiller.yq.formats.Format

import java.nio.file.{Path, Paths}

object Merger {

  implicit val jsonSemigroup: Semigroup[Json] = (x: Json, y: Json) => x.deepMerge(y)

  def apply[F[_]: Sync: ContextShift](loader: FileLoader[F], blocker: Blocker): Command[F] = new Command[F] {
    val parser: Opts[Format => F[Json]] = Opts.subcommand("merge", "merge files in order", true)(Opts.arguments[Path]("files")).map { p =>
      { f: Format =>
        p
          .traverse(loader.loadFile(_, blocker))
          .map { files =>
            files.reduce
          }
          .flatTap { j =>
            Sync[F].delay {
              println(f.print(j))
            }
          }
      }
    }
  }

}
