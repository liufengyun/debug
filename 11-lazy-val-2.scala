object Test {
  lazy val a = {   // jump to end when evaluation
    val b = 8 * 9
    val c = fact(b)
    c + b
  }

  def fact(x: Int): Int = {
    if (x == 0)
      1
    else
      x * fact(x - 1)
  }


   def main(args: Array[String]): Unit = {
    print(a)
  }
}
