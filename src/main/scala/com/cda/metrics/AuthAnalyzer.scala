package com.cda.metrics

import java.time.{Duration, LocalDateTime}
import java.util.concurrent.LinkedBlockingQueue

import com.cda._
import org.slf4j.LoggerFactory

/*
  // TODO
  Queue entries are examined for actual authentication failures. These failures are counted and used to
  generate cloudwatch metrics.
 */

object AuthAnalyzer {

  val logger = LoggerFactory.getLogger(getClass)

  // todo Use configuration file for value
  val threshold = Duration.ofSeconds(30)

  // All auth failures are sent to this AuthAnalyzer.
  // The 'token' is unique for a request and is used to match any
  // subsequent failures occurring within a predetermined time window.
  case class FailureEvent(timestamp: LocalDateTime, token: String)

  case class FailureCount(timestamp: LocalDateTime, count: Int = 0)

  private val queue = new LinkedBlockingQueue[FailureEvent]

  // todo Synchronized map?
  private var map = Map.empty[String, FailureCount]

  private def withinThreshold(start: LocalDateTime, end: LocalDateTime): Boolean =
    Duration.between(start, end).compareTo(threshold) <= 0


  private var active = true

  def enqueue(failure: FailureEvent): Unit = {
    logger.info(s"Enqueueing: $failure")
    queue.put(failure)
  }

  def run(): Unit = {
    logger.info(s"${getClass.getName} is starting")
    active = true
    new Thread(runnable).start()
  }

  def shutdown(): Unit = active = false

  /*
    If no entry with a matching token is found, create an entry with a count of zero.
    If an entry with a matching token is found and the timestamps are within the threshold,
    increment the count and update the timestamp.
    If a entry with a matching token is found and the timestamps are NOT within the threshold,
    leave the count as is, but update the timestamp.
   */
  private def update(failure: FailureEvent): Unit = map.get(failure.token) match {
    case Some(count) =>
      if (withinThreshold(count.timestamp, failure.timestamp))
        map += (failure.token -> FailureCount(failure.timestamp, count.count + 1))
      else map += (failure.token -> FailureCount(failure.timestamp, count.count))

    case None => map += (failure.token -> FailureCount(failure.timestamp))
  }


  // TODO
  // Create and schedule task to run every 5 minutes to create failed authentication metrics


  private val runnable = new Runnable {
    override def run(): Unit = {
      while (active) try {
        update(queue.take())
        logger.info(s"analyzer status: $map")
      } catch {
        case t: Throwable =>
          logger.error(asString(t))
      }

      logger.info(s"${getClass.getName} shutting down...")
    }
  }

}
