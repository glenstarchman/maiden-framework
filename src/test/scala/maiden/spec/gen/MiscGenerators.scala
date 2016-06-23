package maiden.spec.gen

import org.scalacheck.Gen

trait MiscGenerators {
  val genHexChar = Gen.oneOf('a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

  def genHexCharsOfLength(n: Int): Gen[List[Char]] = for {x <- Gen.listOfN(n, genHexChar)} yield x

  def genHexOfLength[T](n: Int)(f: String => T): Gen[T] = Generators.genHexCharsOfLength(n).map(cs => f(cs.mkString))
}
