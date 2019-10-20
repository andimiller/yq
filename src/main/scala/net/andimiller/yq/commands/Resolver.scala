package net.andimiller.yq.commands

import java.nio.file.{Path, Paths}

import cats._
import cats.data.NonEmptyList
import cats.implicits._
import cats.effect._
import com.monovore.decline._
import higherkindness.droste.AlgebraM
import higherkindness.droste.scheme.cataM
import io.circe.Json
import io.circe.rs.JsonF
import net.andimiller.yq.Utils.FileLoader
import net.andimiller.yq.formats.Format
import atto._
import Atto._

object Resolver {

  object JsonStringPath {
    def unapply(j: Json): Option[Traversal.Reference] = j.asString.flatMap { s =>
      Traversal.parse(s).toOption
    }
  }

  object Traversal {
    sealed trait TraversalStep
    case class DownField(s: String) extends TraversalStep
    case class DownIndex(i: Int)    extends TraversalStep

    case class Reference(path: Path, traversal: List[TraversalStep])
    object Parser {
      val file  = takeWhile(_ != '#')
      val index = int
      val field = takeWhile(_ != '/')
      val step: Parser[TraversalStep] =
        (index.map(DownIndex).widen[TraversalStep] || field.map(DownField).widen[TraversalStep]).map(_.merge)

      val traversal: Parser[Option[NonEmptyList[TraversalStep]]] = opt(
        char('#') *> char('/') *> sepBy1(step, char('/'))
      )

      val reference: Parser[Reference] = for {
        f <- file
        t <- traversal.map(_.toList.flatMap(_.toList))
      } yield Reference(Paths.get(f), t)
    }

    def parse(s: String): Either[String, Reference] = Parser.reference.parseOnly(s).either

    def traverse(j: Json, traversals: List[TraversalStep]): Option[Json] = {
      traversals
        .foldLeft(j.hcursor.asInstanceOf[io.circe.ACursor]) {
          case (c, DownField(f)) => c.downField(f)
          case (c, DownIndex(i)) => c.downN(i)
        }
        .focus
    }
  }

  import net.andimiller.yq.droste._

  def resolver[F[_]: Applicative](loader: FileLoader[F], blocker: Blocker): AlgebraM[F, JsonF, Json] = AlgebraM[F, JsonF, Json] {
    case JsonF.JObjectF(Vector(("$ref", JsonStringPath(Traversal.Reference(path, traversal))))) =>
      loader.loadFile(path, blocker).map { j =>
        Traversal.traverse(j, traversal).getOrElse(Json.Null)
      }
    case jf => JsonF.foldJson(jf).pure[F]
  }

  def apply[F[_]: Sync: ContextShift](loader: FileLoader[F], blocker: Blocker): Command[F] = new Command[F] {
    val parser: Opts[Format => F[Json]] = Opts.subcommand("resolve", "resolve references in a file", true)(Opts.argument[Path]("file")).map { p =>
      { f: Format =>
        loader
          .loadFile(p, blocker)
          .flatMap { j =>
            cataM(resolver[F](loader, blocker)).apply(j)
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
