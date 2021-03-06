package org.tksfz.doto.main

import java.io.File

import scopt.{OptionDef, Read}

case class Config(cmd: Option[Cmd] = None)

sealed trait Cmd
case class Init(location: Option[File]) extends Cmd
case class Add(parentId: String, subject: String) extends Cmd
case class ThreadCmd(parentId: String, subject: String, isEvent: Boolean = false) extends Cmd
case class ListCmd(ignoreFocus: Boolean = false) extends Cmd
case class Complete(id: String) extends Cmd
case class Set(id: String, newSubject: Option[String] = None, newParent: Option[String] = None) extends Cmd
case class Plan(taskId: String, eventId: String) extends Cmd
case class Focus(id: String) extends Cmd

trait CmdExec[T] {
  def execute(c: Config, cmd: T): Unit
}

object Main {

  def main(args: Array[String]): Unit = {
    parser.parse(args, Config()) match {
      case Some(c@Config(Some(cmd))) => cmd match {
        case add: Add => AddCmdExec.execute(c, add)
        case thread: ThreadCmd => ThreadCmdExec.execute(c, thread)
        case ls: ListCmd => ListCmdExec.execute(c, ls)
        case init: Init => InitCmdExec.execute(c, init)
        case complete: Complete => CompleteCmdExec.execute(c, complete)
        case set: Set => SetCmdExec.execute(c, set)
        case plan: Plan => PlanCmdExec.execute(c, plan)
        case focus: Focus => FocusCmdExec.execute(c, focus)
        case _ => println(c)
      }
      case Some(config) =>
        println(config)
      case None =>
    }
  }

  val parser = new scopt.OptionParser[Config]("doto") {
    head("doto", "0.1")

    cmd("init").action((_, c) => c.copy(cmd = Some(Init(None))))
      .text("Initialize a new doto repo.")
      .children(
        arg[File]("location").optional().cmdaction[Init]((x, c) => c.copy(location = Some(x)))
      )

    note("")
    cmd("add").action((_, c) => c.copy(cmd = Some(Add("", ""))))
      .text("Add a task or event")
      .children(
        opt[String]('p', "parent").valueName("parent").cmdaction[Add]((x, c) => c.copy(parentId = x)),
        arg[String]("subject").cmdaction[Add]((x, c) => c.copy(subject = x))
      )

    note("")
    cmd("thread").action((_, c) => c.copy(cmd = Some(ThreadCmd("", ""))))
      .text("Add a thread")
      .children(
        opt[String]('p', "parent").valueName("parent").cmdaction[ThreadCmd]((x, c) => c.copy(parentId = x)),
        opt[Unit]('e', "event").cmdaction[ThreadCmd]((x, c) => c.copy(isEvent = true)),
        arg[String]("subject").cmdaction[ThreadCmd]((x, c) => c.copy(subject = x))
      )

    note("")
    cmd("ls").action((_, c) => c.copy(cmd = Some(ListCmd())))
      .text("List objects")
      .children(
        opt[Unit]("no-focus").cmdaction[ListCmd]((x, c) => c.copy(ignoreFocus = true))
      )

    note("")
    cmd("complete").action((_, c) => c.copy(cmd = Some(Complete(""))))
      .text("Mark a task, event, or thread as completed")
      .children(
        arg[String]("id").cmdaction[Complete]((x, c) => c.copy(id = x))
      )

    note("")
    cmd("set").action((_, c) => c.copy(cmd = Some(Set(""))))
      .text("Set a new subject on a task, event, or thread")
      .children(
        arg[String]("id").cmdaction[Set]((x, c) => c.copy(id = x)),
        opt[String]('p', "parent").valueName("parent").cmdaction[Set]((x, c) => c.copy(newParent = Some(x))),
        arg[String]("subject").optional().cmdaction[Set]((x, c) => c.copy(newSubject = Some(x)))
      )

    note("")
    cmd("plan").action((_, c) => c.copy(cmd = Some(Plan("", ""))))
      .text("Target a task to be completed for the specified event")
      .children(
        arg[String]("taskId").cmdaction[Plan]((x, c) => c.copy(taskId = x)),
        arg[String]("eventId").cmdaction[Plan]((x, c) => c.copy(eventId = x))
      )

    note("")
    cmd("focus").action((_, c) => c.copy(cmd = Some(Focus(""))))
      .text("Put a thread into focus")
      .children(
        arg[String]("id").cmdaction[Focus]((x, c) => c.copy(id = x))
      )
  }

  implicit class OptionDefExtensions[A: Read](d: OptionDef[A, Config]) {
    def cmdaction[T <: Cmd](f: (A, T) => T) = {
      d.action((x, c) =>
        c.copy(cmd = c.cmd.map(cmd => f(x, cmd.asInstanceOf[T])))
      )
    }
  }
}
