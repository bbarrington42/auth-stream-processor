package com.cda.records

import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.model.Record
import com.cda._
import com.cda.metrics.AuthAnalyzer
import com.cda.metrics.AuthAnalyzer.FailureEvent
import org.slf4j.LoggerFactory
import scalaz.\/

import scala.collection.JavaConverters._

/*
  This component receives all entries of the consumer application log via a Kinesis stream. Each entry is checked
  against a regular expression to determine if it is a janrain failure response to an auth request. If a failure
  response is received, a FailureEvent is created and sent to the AuthAnalyzer via a blocking queue.
  Note that a more 'production ready' architecture would sent events to an SQS queue. Also this process should probably
  run on multiple hosts behind a load balancer.
 */

// Must be a class since the RecordProcessorFactory creates a new instance for every invocation of 'createProcessor'
class RecordProcessor extends IRecordProcessor {

  val logger = LoggerFactory.getLogger(getClass)

  // This is an example of a log record we are trying to match:
  //
  // 2018-05-07 19:30:53,767 [ INFO] .c.f.c.s.u.JanrainService {ForkJoinPool-3-worker-3} -> janrain auth response:
  // status=error, token=gb5hxgc5sthrag4z, response={"request_id":"x955wtqpusset6a9","code":200,
  // "error_description":"unknown access token","error":"invalid_argument","stat":"error"}

  val authResponseRegex = """(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}).+janrain auth response: status=error, token=([^,]+)""".r

  val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  val decoder = Charset.forName("UTF-8").newDecoder

  // Apply regex and convert to FailureEvent if there is a match
  private def find(text: String): Option[FailureEvent] =
    authResponseRegex.findFirstMatchIn(text).map(m =>
      FailureEvent(LocalDateTime.parse(m.group(1), datePattern), m.group(2)))

  // Interpret the payload as UTF-8 chars.
  private def find(record: Record): Throwable \/ Option[FailureEvent] =
    \/.fromTryCatchNonFatal(find(decoder.decode(record.getData).toString))


  override def initialize(initializationInput: InitializationInput): Unit =
    logger.info(s"Initializing... ${initializationInput.getShardId}")


  // Filter for errors and send them to the analyzer
  override def processRecords(input: ProcessRecordsInput): Unit = {
    logger.info(s"Processing ${input.getRecords.size()} records")
    input.getRecords.asScala.foreach(find(_).fold(
      thr => logger.error(s"Record processing error - ${asString(thr)}"),
      _.foreach(AuthAnalyzer.enqueue(_))))
    try {
      input.getCheckpointer.checkpoint()
    } catch {
      case thr: Throwable =>
        logger.error(s"Checkpoint failed - ${asString(thr)}")
    }
  }


  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    val reason = shutdownInput.getShutdownReason
    logger.info(s"Shutting down, reason: $reason")
    AuthAnalyzer.shutdown()

    if (ShutdownReason.TERMINATE == reason)
      shutdownInput.getCheckpointer.checkpoint()
  }

}

