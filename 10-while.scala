object Test {

  def main(args: Array[String]): Unit = {
    var a = 1 + 2
    a = a + 3
    a = 4 + 5

    while (a * 8 < 100) { // first jump to end
      a += 1
    }

    print(a)
  }
}
