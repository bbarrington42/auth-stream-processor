package com.cda.metrics

import java.time.{LocalDateTime, Duration => jDuration}
import java.util.concurrent.LinkedBlockingQueue

import com.cda._
import org.slf4j.LoggerFactory
import scalaz.-\/
import scalaz.concurrent.Task

import scala.concurrent.duration._

/*
  // TODO
  Queue entries are examined for actual authentication failures. These failures are counted and used to
  generate cloudwatch metrics.
 */

object AuthAnalyzer {

  val logger = LoggerFactory.getLogger(getClass)

  // todo Use configuration file for values
  val threshold = jDuration.ofSeconds(30)
  val metricsInterval = Duration(1, MINUTES)


  // All auth failures are sent to this AuthAnalyzer.
  // The 'token' is unique for a request and is used to match any
  // subsequent failures occurring within a predetermined time window.
  case class FailureEvent(timestamp: LocalDateTime, token: String)

  case class FailureCount(timestamp: LocalDateTime, count: Int = 0)

  private val queue = new LinkedBlockingQueue[FailureEvent]

  // todo Synchronized map?
  private var map = Map.empty[String, FailureCount]

  private def withinThreshold(start: LocalDateTime, end: LocalDateTime): Boolean =
    jDuration.between(start, end).compareTo(threshold) <= 0


  private var active = true

  def enqueue(failure: FailureEvent): Unit = queue.put(failure)

  def shutdown(): Unit = active = false

  /*
    If no entry with a matching token is found, create an entry with a count of zero.
    If an entry with a matching token is found and the timestamps are within the threshold,
    increment the count and update the timestamp.
    If a entry with a matching token is found and the timestamps are NOT within the threshold,
    leave the count as is, but update the timestamp.
   */
  private def update(failure: FailureEvent): Unit = map.synchronized {
    map.get(failure.token) match {
      case Some(count) =>
        if (withinThreshold(count.timestamp, failure.timestamp))
          map += (failure.token -> FailureCount(failure.timestamp, count.count + 1))
        else map += (failure.token -> FailureCount(failure.timestamp, count.count))

      case None => map += (failure.token -> FailureCount(failure.timestamp))
    }
  }

  // Obtain a copy of the map containing all entries with a count > 0 and reset the map
  private def resetMap(): Map[String, FailureCount] = map.synchronized {
    val filtered = map.filter { case (_, failure) => failure.count > 0 }
    map = map.empty
    filtered
  }

  // Create and schedule task to run periodically to create failed authentication metrics
  private def scheduleMetrics(): Unit =
    Task.schedule(doMetrics(), metricsInterval).unsafePerformAsync(_ match {
      case -\/(thr) => logger.error(s"Metrics calc failed - ${asString(thr)}")
      case _ =>
    })

  private def doMetrics(): Unit = {
    val failures = resetMap()
    val count = failures.foldLeft(0) { case (z, (_, fc)) => z + fc.count }
    logger.info(s"$count auth failures in $metricsInterval")
    scheduleMetrics()
  }


  private val runnable = new Runnable {
    override def run(): Unit = {
      while (active) try {
        update(queue.take())
        logger.info(s"analyzer status: $map")
      } catch {
        case t: Throwable =>
          logger.error(s"Map update failed - ${asString(t)}")
      }

      logger.info(s"${getClass.getName} shutting down...")
    }
  }

  new Thread(runnable).start()

  scheduleMetrics()
}
