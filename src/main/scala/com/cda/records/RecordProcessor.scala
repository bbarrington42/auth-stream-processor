package com.cda.records

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.model.Record
import com.cda.metrics.AuthAnalyzer
import com.cda.metrics.AuthAnalyzer.FailureEvent

import scala.collection.JavaConverters._

/*
  Extract occurrences of responses to janrain auth requests and sends them to AuthAnalyzer.
 */
object RecordProcessor extends IRecordProcessor {

  // This is an example of a log record we are trying to match
  // 2018-05-07 19:30:53,767 [ INFO] .c.f.c.s.u.JanrainService {ForkJoinPool-3-worker-3} -> janrain auth response: status=error, token=gb5hxgc5sthrag4z, response={"request_id":"x955wtqpusset6a9","code":200,"error_description":"unknown access token","error":"invalid_argument","stat":"error"}
  val authResponseRegex =
  """(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}).+janrain auth response: status=error, token=([^,]+)""".r

  val datePattern = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  val zone = ZoneId.of("America/New_York")

  // TODO Make sure this will work for all cases
  private def toString(bb: ByteBuffer): String = {
    val bytes = Array.ofDim[Byte](bb.remaining)
    new String(bytes, Charset.defaultCharset)
  }

  private def toDate(text: String): LocalDateTime =
    LocalDateTime.parse(text, datePattern)

  private def find(s: String): Option[FailureEvent] =
    authResponseRegex.findFirstMatchIn(s).map(m =>
      FailureEvent(toDate(m.group(1)), m.group(2)))

  private def find(record: Record): Option[FailureEvent] = find(toString(record.getData))

  // todo
  override def initialize(initializationInput: InitializationInput): Unit = ???

  // Filter for errors and send them to the analyzer
  override def processRecords(input: ProcessRecordsInput): Unit =
    input.getRecords.asScala.foreach(find(_).foreach(AuthAnalyzer.enqueue(_)))


  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    AuthAnalyzer.shutdown()

    // todo
  }

  def main(args: Array[String]): Unit = {
    val s = "2018-05-07 19:31:01,767 [ INFO] .c.f.c.s.u.JanrainService {ForkJoinPool-3-worker-3} -> janrain auth response: status=error, token=gb5hxgc5sthrag4z, response={\"request_id\":\"x955wtqpusset6a9\",\"code\":200,\"error_description\":\"unknown access token\",\"error\":\"invalid_argument\",\"stat\":\"error\"}"
    println(find(s))
  }
}


object RecordProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = RecordProcessor
}
