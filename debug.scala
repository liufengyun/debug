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

abstract class Command(val name: String) extends Tree
case class SimpleCommand(cmd: String) extends Command(cmd)
case class PatternCommand(cmd: String, pattern: String) extends Command(cmd)
case class RegexCommand(cmd: String, regex: String) extends Command(cmd)
case class ExactCommand(cmd: String, expect: String) extends Command(cmd)

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
    SimpleCommand(line.trim)
  } else {
    val Seq(cmd, rhs) = line.split(":", 2).toSeq.map(_.trim)
    if (rhs.isEmpty) { // read next line
      if (lines.size == 0) error("unexpected end of file. Specification for `" + cmd + "` required")

      if (lines.head.trim.startsWith("\"\"\"")) {    // exact match, can be multiple lines
         val line = lines.remove(0).trim
         val content = "\"\"\"(.+)\"\"\"".r
         line match {
             case content(expect) => ExactCommand(cmd, expect)
             case _ =>
               val buffer = new ListBuffer[String]
               while (lines.size > 0 && lines.head.trim != "\"\"\"") buffer += lines.remove(0)

               if (lines.size == 0) error("unexpected end of file. Specification for `" + cmd + "` incomplete. Ending \"\"\" expected.")

               lines.remove(0)
               ExactCommand(cmd, buffer.mkString("\\n"))
         }
      } else if (lines.head.trim.startsWith("\"")) {               // regex match, must be just one line
         val line = lines.remove(0).trim
         val content = "\"(.+)\"".r
         line match {
             case content(expect) => RegexCommand(cmd, expect)
             case _ => error("incomplete specification: `" + line + "` for `" + cmd + "`. Ending \" expected.")
          }
      } else {                                        // simple match, must be just one line, starting/ending spaces ignored
        PatternCommand(cmd, lines.remove(0).trim)
      }

    } else { // inline expected
      if (rhs.startsWith("\"\"\"")) {    // exact match
         val content = "\"\"\"(.+)\"\"\"".r
         rhs match {
             case content(expect) => ExactCommand(cmd, expect)
             case _ => error("incorrect specification: `" + rhs + "` for `" + cmd + "`. Ending \"\"\" expected.")
         }
      } else if (rhs.startsWith("\"")) {  // regex match
         val content = "\"(.+)\"".r
         rhs match {
             case content(expect) => RegexCommand(cmd, expect)
             case _ => error("incorrect specification: `" + rhs + "` for `" + cmd + "`. Ending \" expected.")
          }
      } else {                           // simple match
         PatternCommand(cmd, rhs)
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

def generate(check: CheckFile): Unit = ???

val lines = Source.fromFile(args(0)).getLines.toBuffer

println(lines.mkString("\n"))

println(parse(lines))
// generate(parse(lines))

