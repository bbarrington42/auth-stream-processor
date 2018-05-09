package com.cda.metrics

import java.time.{LocalDateTime, Duration => jDuration}
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MINUTES

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder
import com.amazonaws.services.cloudwatch.model.{Dimension, StandardUnit}
import com.cda._
import com.cocacola.freestyle.cda.util.cloudwatch.{CloudWatchMetricPublisher, CloudWatchValueMetric}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

/*
  This component receives FailureEvents (janrain authentication failures) and enqueues them on a blocking queue.
  Events are retrieved and a Map of counts keyed by token is updated. A true authentication failure is assumed to
  have occurred if there are two failure responses for the same token within the time threshold.
  A periodic task runs which takes a filtered copy of the Map. The copy contains only those entries having a count > 0.
  The copied Map is then replaced with an empty one. The counts of the Map copy are tallied and a Metric is generated.

  Note that a more 'production ready' architecture would have this component receiving events from an SQS queue.
 */

object AuthAnalyzer {

  val logger = LoggerFactory.getLogger(getClass)

  // todo Use configuration file for values
  val threshold = jDuration.ofSeconds(30)
  val metricsInterval = Duration(1, MINUTES)

  // todo Use role to get the default client
  val credentialsProvider = new ProfileCredentialsProvider("cda")
  val cloudWatchClient = AmazonCloudWatchClientBuilder.standard().withCredentials(credentialsProvider).build()


  val authFailures = new CloudWatchValueMetric {
    val instanceId = sys.props.get("instance.id").getOrElse("<instance-id>")

    override def value: Double = getFailures.foldLeft(0) { case (z, (_, fc)) => z + fc.count }

    override val metricName: String = "JanrainAuthFailures"
    override val unit: StandardUnit = StandardUnit.Count
    override val namespace: String = "Consumer"
    override val dimensions: Seq[Dimension] =
      Seq((new Dimension).withName("InstanceId").withValue(instanceId))
  }

  private val queue = new LinkedBlockingQueue[FailureEvent]

  private val runnable = new Runnable {
    override def run(): Unit = {
      while (active) try {
        update(queue.take())
        logger.info(s"analyzer status: $map")
      } catch {
        case t: Throwable =>
          logger.error(s"Map update failed - ${asString(t)}")
      }

      logger.error(s"${getClass.getName} is exiting...")
    }
  }
  
  private var map = Map.empty[String, FailureCount]

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

  private def withinThreshold(start: LocalDateTime, end: LocalDateTime): Boolean =
    jDuration.between(start, end).compareTo(threshold) <= 0

  // Obtain a copy of the map containing all entries with a count > 0 and reset the map
  private def getFailures(): Map[String, FailureCount] = map.synchronized {
    val filtered = map.filter { case (_, failure) => failure.count > 0 }
    map = map.empty
    filtered
  }

  // All auth failures are sent to this AuthAnalyzer.
  // The 'token' is unique for a request and is used to match any
  // subsequent failures occurring within a predetermined time window.
  case class FailureEvent(timestamp: LocalDateTime, token: String)

  case class FailureCount(timestamp: LocalDateTime, count: Int = 0)


  // Register this metric with the publisher
  CloudWatchMetricPublisher(cloudWatchClient).register(authFailures, 0, 5, MINUTES)

  // Run queue consumer task
  new Thread(runnable).start()

}
