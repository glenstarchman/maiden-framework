package maiden.util.async

import java.util.concurrent.Executors.newFixedThreadPool
import java.util.concurrent.TimeUnit._

import com.twitter.util.{Future, FuturePool}
import maiden.config.Config
import maiden.util.log.Logger.log

import scala.concurrent.ExecutionContext

trait AsyncOps {
  lazy val executorService = newFixedThreadPool(Config.miscThreadPoolSize)
  lazy val futurePool = FuturePool.interruptible(executorService)
  lazy val globalAsyncExecutionContext: ExecutionContext = scala.concurrent.ExecutionContext.fromExecutor(executorService)

  sys.addShutdownHook(shutdownExecutorService())

  def runAsync[T](f: => T): Future[T] = futurePool.apply(f)

  def shutdownExecutorService(): Unit = {
    log.info("Shutting down executor service...")
    executorService.shutdown()
    try {
      executorService.awaitTermination(10L, SECONDS)
    } catch {
      case e: InterruptedException => {
        log.warn("Interrupted while waiting for graceful shutdown, forcibly shutting down...")
        executorService.shutdownNow()
      }
    }
  }
}

object AsyncOps extends AsyncOps
