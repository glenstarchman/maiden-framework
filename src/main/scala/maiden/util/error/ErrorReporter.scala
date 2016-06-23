package maiden.util.error

import com.rollbar.Rollbar
import maiden.config.Config._
import maiden.config.Environment
import maiden.util.async.AsyncOps.runAsync
import maiden.util.config.Environment

trait ErrorReporter {
  def registerForUnhandledExceptions(): Unit

  def info(t: Throwable): Unit

  def warning(t: Throwable): Unit

  def error(t: Throwable): Unit
}

final class RollbarErrorReporter(accessToken: String, environment: Environment) extends ErrorReporter {
  private lazy val rollbar = new Rollbar(accessToken, environment.name)

  override def registerForUnhandledExceptions() = rollbar.handleUncaughtErrors()

  override def info(t: Throwable) = runAsync(rollbar.info(t))

  override def warning(t: Throwable) = runAsync(rollbar.warning(t))

  override def error(t: Throwable) = runAsync(rollbar.error(t))
}

object ErrorReporter {
  val errorReporter: ErrorReporter = new RollbarErrorReporter(rollbarAccessKey, Environment.env)
}
