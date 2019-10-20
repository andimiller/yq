package net.andimiller.yq

import higherkindness.droste.{Algebra, Basis, Coalgebra}
import io.circe.Json
import io.circe.rs.JsonF

object droste {
  val jsonAlgebra: Algebra[JsonF, Json]      = Algebra(JsonF.foldJson)
  val jsonCoalgebra: Coalgebra[JsonF, Json]  = Coalgebra(JsonF.unfoldJson)
  implicit val jsonBasis: Basis[JsonF, Json] = Basis.Default(jsonAlgebra, jsonCoalgebra)
}
