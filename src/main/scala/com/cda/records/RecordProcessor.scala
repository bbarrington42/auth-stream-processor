package com.cda.records

import java.nio.ByteBuffer
import java.nio.charset.Charset

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.types.{InitializationInput, ProcessRecordsInput, ShutdownInput}
import com.amazonaws.services.kinesis.model.Record
import com.cda.metrics.AuthAnalyzer
import com.cda.metrics.AuthAnalyzer.FailureEvent

import scala.collection.JavaConverters._

/*
  Filters occurrences of responses to janrain auth requests and sends them to AuthAnalyzer.
 */
object RecordProcessor extends IRecordProcessor {

  // This is an example of a log record we are trying to match
  // janrain auth request: status=error, token=gb5hxgc5sthrag4z, response={"request_id":"x955wtqpusset6a9","code":200,"error_description":"unknown access token","error":"invalid_argument","stat":"error"}

  val authResponseRegex = """janrain auth request: status=error, token=([^,]+)""".r

  // TODO Make sure this will work for all cases
  private def toString(bb: ByteBuffer): String = {
    val bytes = Array.ofDim[Byte](bb.remaining)
    new String(bytes, Charset.defaultCharset)
  }

  private def find(s: String): Option[FailureEvent] =
    authResponseRegex.findFirstMatchIn(s).map(m =>
      FailureEvent(System.currentTimeMillis(), m.group(1)))

  private def find(record: Record): Option[FailureEvent] = find(toString(record.getData))

  override def initialize(initializationInput: InitializationInput): Unit = ???

  override def processRecords(input: ProcessRecordsInput): Unit = {
    // Filter for errors and send them to the analyzer
    input.getRecords.asScala.foreach(find(_).foreach(AuthAnalyzer.enqueue(_)))
  }

  override def shutdown(shutdownInput: ShutdownInput): Unit = {
    AuthAnalyzer.shutdown()

    // todo
  }
}


object RecordProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor(): IRecordProcessor = RecordProcessor
}
