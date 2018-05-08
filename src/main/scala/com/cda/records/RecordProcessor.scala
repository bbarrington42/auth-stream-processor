package com.cda.records

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.model.Record
import com.cda._
import com.cda.metrics.AuthAnalyzer
import com.cda.metrics.AuthAnalyzer.FailureEvent
import org.slf4j.LoggerFactory
import scalaz.{-\/, \/, \/-}

import scala.collection.JavaConverters._

/*
  Extract occurrences of responses to janrain auth requests and sends them to AuthAnalyzer.
 */
object RecordProcessor extends IRecordProcessor {

  val logger = LoggerFactory.getLogger(getClass)

  // This is an example of a log record we are trying to match
  // 2018-05-07 19:30:53,767 [ INFO] .c.f.c.s.u.JanrainService {ForkJoinPool-3-worker-3} -> janrain auth response: status=error, token=gb5hxgc5sthrag4z, response={"request_id":"x955wtqpusset6a9","code":200,"error_description":"unknown access token","error":"invalid_argument","stat":"error"}
  val authResponseRegex =
  """(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}).+janrain auth response: status=error, token=([^,]+)""".r

  val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  val decoder = Charset.forName("UTF-8").newDecoder

  // For this app, we interpret the payload as UTF-8 chars.
  private def toString(bb: ByteBuffer): Throwable \/ String =
    \/.fromTryCatchNonFatal {
      decoder.decode(bb).toString
    }

  private def find(text: String): Option[FailureEvent] = {
    val option = authResponseRegex.findFirstMatchIn(text).map(m =>
      FailureEvent(LocalDateTime.parse(m.group(1), datePattern), m.group(2)))

    option.foreach(event => logger.info(event.toString))
    option
  }

  private def find(record: Record): Option[FailureEvent] = {
    toString(record.getData).fold(thr => {
      logger.error(asString(thr))
      None
    }, find)
  }


  override def initialize(initializationInput: InitializationInput): Unit =
    logger.info(s"Initializing processor for shard: ${initializationInput.getShardId}")


  // Filter for errors and send them to the analyzer
  override def processRecords(input: ProcessRecordsInput): Unit = {
    logger.info(s"Processing ${input.getRecords.size()} records")
    input.getRecords.asScala.foreach(find(_).foreach(AuthAnalyzer.enqueue(_)))
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


object RecordProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = RecordProcessor
}
