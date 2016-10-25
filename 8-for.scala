object Test {
  def f(): Unit = {
    val a = for (i <- 1 to 5; j <- 10 to 20)
              yield (i, j)      // Error: incorrect reaching this line

    for (i <- 1 to 5; j <- 10 to 20)
      println(i + j)           // TODO: i is renamed to i$2 --> reduce debuggability
  }


   def main(args: Array[String]): Unit = {
     val b = 8 * 9
     f()
     20 + b
     print(b)
  }
}
