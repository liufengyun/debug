#!/bin/sh
exec scala -savecompiled "$0" "$@"
!#

import scala.io.Source
import scala.collection.mutable.Buffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.StringBuilder

sealed trait Tree

sealed abstract class Config(val name: String, val value: String) extends Tree
case class Break(lines: Seq[Int]) extends Config("break", lines.mkString(", "))

case class Command(val name: String, val expect: Expect = EmptyExpect) extends Tree

sealed trait Expect extends Tree
case object EmptyExpect extends Expect
case class LitExpect(lit: String) extends Expect
case class PatExpect(pat: String) extends Expect

case class CheckFile(configs: Seq[Config], commands: Seq[Command])

def error(msg: String): Nothing = {
  throw new Exception(msg)
}

def parseBreak(line: String): Break = {
  val Seq("break", points) = line.split(":", 2).toSeq
  Break(points.split(',').map(_.trim.toInt))
}

def parseCommand(lines: Buffer[String]): Command = {
  val line = lines.remove(0).split("#", 2).head  // remove trailing comment

  val index = line.indexOf(':')
  if (index == -1) { // simple command
    Command(line.trim)
  } else {
    val Seq(cmd, rhs) = line.split(":", 2).toSeq.map(_.trim)
    if (rhs.isEmpty) { // read next line
      if (lines.size == 0) error("unexpected end of file. Specification for `" + cmd + "` required")

      if (lines.head.trim.startsWith("\"")) {               // regex match, must be just one line
         val line = lines.remove(0).trim
         val content = "\"(.+)\"".r
         line match {
             case content(expect) => Command(cmd, PatExpect(expect))
             case _ => error("incomplete specification: `" + line + "` for `" + cmd + "`. Ending \" expected.")
          }
      } else {                                        // pattern match, must be just one line, starting/ending spaces ignored
        Command(cmd, LitExpect(lines.remove(0).trim))
      }

    } else { // inline expected
      if (rhs.startsWith("\"")) {  // regex match
         val content = "\"(.+)\"".r
         rhs match {
             case content(expect) => Command(cmd, PatExpect(expect))
             case _ => error("incorrect specification: `" + rhs + "` for `" + cmd + "`. Ending \" expected.")
          }
      } else {                           // pattern match
         Command(cmd, LitExpect(rhs))
      }
    }
  }
}

def parse(lines: Buffer[String]): CheckFile = {
  val (config, body) = {
    if (lines(0) == "---") {
      lines.remove(0)
      val index = lines.indexWhere(_ == "---")
      if (index == -1) error("front matter must end with `---`.")

      lines.splitAt(index) match {
        case (front, body) =>
          body.remove(0)
          (front, body)
      }
    } else (Nil, lines)
  }

  val configs = config.map(parseBreak)
  val cmds = {
    val cmds = new ListBuffer[Command]()
    while (body.size > 0) {
      if (body.head.startsWith("#") || body.head.trim.isEmpty)
        body.remove(0)
      else
        cmds += parseCommand(body)
    }

    cmds
  }

  CheckFile(configs, cmds)
}

def generate(check: CheckFile): Unit = {
  val CheckFile(configs, cmds) = check
  val breakpoints = (configs.flatMap {
    case Break(points) => points.map(x => s"stop at Test:$x")
  }).mkString("\n")

  val commands = (cmds.map {
    case Command(cmd, EmptyExpect)         => s"""send "$cmd""""
    case Command(cmd, LitExpect(lit))   => s"""send "$cmd"\nexpect "$lit""""
    case Command(cmd, PatExpect(pat))   => s"""send "$cmd"\nexpect -re {$pat}"""
  }).mkString("\n\n")

  println(
s"""
#!/usr/bin/expect

log_user 1
set timeout 9

spawn jdb -attach 5005

# breakpoints
$breakpoints

# interactions
$commands
""")
}

val lines = Source.fromFile(args(0)).getLines.toBuffer

// println(lines.mkString("\n"))
// println(parse(lines))

generate(parse(lines))
