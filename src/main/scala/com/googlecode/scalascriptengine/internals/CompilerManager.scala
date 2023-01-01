package com.googlecode.scalascriptengine.internals

import java.io.File
import com.googlecode.scalascriptengine.{Logging, ScalaScriptEngine, SourcePath}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.reflect.internal.util.Position
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.{Global, Phase, Settings, SubComponent}

/**
 * manages the scala compiler, taking care of setting the correct compiler parameters
 * and reporting errors.
 *
 * @author kostantinos.kougios
 *
 *         22 Dec 2011
 */
class CompilerManager(sourcePaths: List[SourcePath], classPaths: Set[File], sse: ScalaScriptEngine) extends Logging {

  private def acc(todo: List[SourcePath], done: List[SourcePath]): List[(SourcePath, (Global, Global#Run, CompilationReporter))] = todo match {
    case Nil => Nil
    case h :: t =>
      val settings = new Settings(s => {
        error("errors report: " + s)
      })
      settings.sourcepath.tryToSet(h.sources.map(_.getAbsolutePath).toList)
      val cp = done.map(_.targetDir) ++ classPaths
      settings.classpath.tryToSet(List(cp.map(_.getAbsolutePath).mkString(File.pathSeparator)))
      settings.outdir.tryToSet(h.targetDir.getAbsolutePath :: Nil)

      val reporter = new CompilationReporter(settings)

      class SSEGlobal extends Global(settings, reporter) {

        object SSEPhase extends SubComponent {
          val global: SSEGlobal.this.type = SSEGlobal.this
          val phaseName = "SSEPhase"
          val runsAfter = List[String]("typer")
          val runsRightAfter = None

          def newPhase(_prev: Phase): StdPhase = new StdPhase(_prev) {
            def apply(unit: CompilationUnit): Unit = {
              info("compiling unit " + unit)
              sse.compilationStatus.checkStop
            }
          }
        }

        override protected def computePhaseDescriptors: List[SubComponent] = {
          addToPhasesSet(SSEPhase, "SSEPhase")
          super.computePhaseDescriptors
        }
      }

      val g = new SSEGlobal

      val run = new g.Run
      (h, (g, run, reporter)) :: acc(t, h :: done)

  }

  private val runMap = acc(sourcePaths, Nil).toMap

  def compile(allFiles: List[String]): Unit = {

    def doCompile(sp: SourcePath, cp: Set[File]): String
    = {
      val (g, run, reporter) = runMap(sp)

      val rootPaths = sp.sources.map(_.getAbsolutePath)
      val files = allFiles.filter {
        f =>
          rootPaths.exists(rp => f.startsWith(rp))
      }
      run.compile(files)

      val errors = reporter.results
      if (errors.nonEmpty) {
        s"${errors.size} error(s) occurred:\n" +
          s"${errors.map(t => t._1.source.file.canonicalPath + "\n\t" + t._2).mkString("\n")}\n"
      } else {
        ""
      }
    }

    @tailrec
    def all(todo: List[SourcePath], done: List[SourcePath], errorMessage: String = ""): String = {
      todo match {
        case Nil => errorMessage
        // nop
        case h :: t =>
          val currentErrorMessage = doCompile(h, classPaths ++ done.map(_.targetDir))
          all(t, h :: done, errorMessage + " " + currentErrorMessage)
      }
    }

    all(sourcePaths, Nil).trim match {
      case "" =>
      case m  => throw new CompilationError(m)
    }
  }

}

class CompilationError(msg: String) extends RuntimeException(msg)

private class CompilationReporter(val settings: Settings) extends Reporter with Logging {
  private val errors: mutable.Builder[(Position, String), List[(Position, String)]] = List.newBuilder[(Position, String)]

  def results: List[(Position, String)] = errors.result()
  override protected def info0(pos: Position, msg: String, severity: Severity, force: Boolean): Unit = {
    val m = Position.formatMessage(pos, msg, true)
    if (severity == ERROR)
      errors += (pos -> m)
    else warn(m)
  }
}