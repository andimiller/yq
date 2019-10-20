package net.andimiller.yq.commands

import com.monovore.decline.Opts
import io.circe.Json
import net.andimiller.yq.formats.Format

trait Command[F[_]] {
  val parser: Opts[Format => F[Json]]
}
