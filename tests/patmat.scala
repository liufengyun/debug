object Test {
  sealed abstract class CL3Literal
  case class IntLit(i: Int) extends CL3Literal
  case class CharLit(c: Char) extends CL3Literal
  case class BooleanLit(b: Boolean) extends CL3Literal

  def f(tree: CL3Literal) : Unit = tree match {
    case IntLit(x) =>
      println(x)
    case CharLit(x) => println(x)
    case BooleanLit(x) =>
      println(x)
  }

   def main(args: Array[String]): Unit = {
     val a = 1 + 2
     val b = a * 9
     f(IntLit(3))       // incorrect jump to last case and back; and jump back to last case
     f(CharLit('A'))    // incorrect jumping
     f(BooleanLit(false)) // incorrect jumping
  }
}

