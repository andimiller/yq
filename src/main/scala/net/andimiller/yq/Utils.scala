package net.andimiller.yq

import java.nio.file.Path

import cats.implicits._
import cats.effect._
import io.circe.Json
import net.andimiller.yq.formats.Format

object Utils {

  trait FileLoader[F[_]] {
    def loadFile(p: Path, b: Blocker): F[Json]
  }
  object FileLoader {

    def apply[F[_]: Sync: ContextShift](f: Format): FileLoader[F] =
      (p: Path, b: Blocker) =>
        fs2.io.file.readAll(p, b, 2048).through(fs2.text.utf8Decode[F]).compile.foldMonoid.flatMap { s =>
          Sync[F].fromEither(f.parse(s))
      }

  }

}
