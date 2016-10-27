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

def generate(check: CheckFile): String = {
  val CheckFile(configs, cmds) = check
  val breakpoints = (configs.flatMap {
    case Break(points) => points.map(x =>
        s"""
        |send "stop at $mainObj$$:$x\\r"
        |expect "breakpoint $mainObj$$:$x"
        """.stripMargin
    )
  }).mkString("\n\n")

  val commands = (cmds.map {
    case Command(cmd, EmptyExpect)      =>
        s"""
        |send_user "send command `$cmd`\\n"
        |send "$cmd\\r"
        |expect "main"
        """.stripMargin
    case Command(cmd, LitExpect(lit))   =>
        s"""
        |send_user "send command `$cmd`\\n"
        |send "$cmd\\r"
        |expect {
        |   "$lit" { send_user "success - $cmd : $lit \\n" }
        |   timeout {
        |       send_user "timeout while waiting for response: $cmd : $lit\\n"
        |       exit 1
        |    }
        |}
        |""".stripMargin
    case Command(cmd, PatExpect(pat))   =>
        s"""
        |send_user "send command `$cmd`\\n"
        |send "$cmd\\r"
        |expect {
        |   -re {$pat} { send_user "success - $cmd : $pat \\n" }
        |   timeout {
        |       send_user "timeout while waiting for response: $cmd : $pat\\n"
        |       exit 1
        |    }
        |}
        |""".stripMargin
  }).mkString("\n\n")

s"""
#!/usr/bin/expect

# log_user 1
# exp_internal 1
# set timeout 5

send_user "spawning job...\\n"

spawn jdb -attach 5005

send_user "interacting...\\n"

expect {
  "*VM Started*" { send_user "success - connected to server \\n" }
  timeout {
      send_user "timeout while waiting for: *VM Started*\\n"
      exit 1
  }
}

send_user "setting breakpoints...\\n"

# breakpoints
$breakpoints

# run
send_user "run program...\\n"
send "run\\r"
expect "Breakpoint hit"

# interactions
$commands
""".trim
}

val (mainObj, file) =
    if (args.size == 1) ("Test", args(0))
    else if (args(0) == "-m") (args(1), args(2))
    else throw new Exception("incorrect args")

val lines = Source.fromFile(file).getLines.toBuffer

// println(lines.mkString("\n"))
// println(parse(lines))

println(generate(parse(lines)))

