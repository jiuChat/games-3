package vggames.scala.code

import com.twitter.util.Eval
import java.security.Permission
import vggames.scala.specs.GameSpecification
import vggames.scala.tasks.judge.ExecutionFailure
import vggames.shared.task.JudgedTask
import vggames.scala.code.Wrappers._
import vggames.scala.tasks.judge.GameJudger

class ScalaProcessor[T <: CodeRestrictions[_]](spec : GameSpecification[T]) {
  val className = "ExpressionRunner"
  val fullName = "scalagameunsafe.ExpressionRunner"
  
  def processCode(code : String) : JudgedTask = {
    val eval = new Eval(None)
    compile(code, eval)
    run(className, eval)
  }

  def run(className : String, eval : Eval) : JudgedTask = {
    val code = eval.findClass(fullName).newInstance.asInstanceOf[T]
    spec.code = code
    try {
      (new GameJudger(spec)).judgement
    } catch {
      case t => { t.printStackTrace; new ExecutionFailure(t) }
    }
  }

  private def compile(code : String, eval : Eval) = {
    if (code.contains("finally") || code.contains("catch"))
      throw new SecurityException("Tentativa de executar c&oacute;digo privilegiado dentro de uma task.")
    val wrapped = wrap(className, code, spec.extendsType, spec.runSignature)
    eval.compile(wrapped)
  }
}

object TaskRunSecurityManager extends SecurityManager {
  val unsafe = new ThreadLocal[Boolean]

  override def checkPermission(perm : Permission) = handlePermission(perm)
  override def checkPermission(perm : Permission, context : Object) = handlePermission(perm)

  def handlePermission(perm : Permission) =
    if (unsafe.get) throw new SecurityException("Tentativa de executar c&oacute;digo privilegiado dentro de uma task.")
}

object Wrappers {
  def wrap(className : String, code : String, extendsType : String, runSignature : String) = {
    "package scalagameunsafe\n" +
    "import vggames.scala.code._\n" +
    "class " + className + " extends " + extendsType + " {\n" +
    "  def run" + runSignature + " = {\n" +
    code + "\n" +
    "  }\n" +
    "}\n"
  }
}