object Test {
  def fact(x: Int): Int = {
    if (x == 0)
      1
    else
      x * fact(x - 1) // TODO: incorrect this line when x = 0
  }


   def main(args: Array[String]): Unit = {
     val a = 1 + 2
     val b = a * 9
     val c = fact(a)
     print(c)
  }
}
