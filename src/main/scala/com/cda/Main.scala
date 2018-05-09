package com.cda

import java.net.InetAddress
import java.util.UUID

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.cda.records.RecordProcessor
import org.slf4j.LoggerFactory


object Main {

  val logger = LoggerFactory.getLogger(getClass)

  val credentialsProvider = new ProfileCredentialsProvider("cda")
  val workerId = InetAddress.getLocalHost.getCanonicalHostName + ":" + UUID.randomUUID

  val config =
    new KinesisClientLibConfiguration(
      "auth-stream-processor",
      "devLogStream",
      credentialsProvider,
      workerId)

  val recordProcessorFactory = new IRecordProcessorFactory {
    override def createProcessor(): IRecordProcessor = RecordProcessor
  }

  def main(args: Array[String]): Unit = {
    // Ensure the JVM will refresh the cached IP values of AWS resources.
    java.security.Security.setProperty("networkaddress.cache.ttl", "60")

    val worker = new Worker.Builder().recordProcessorFactory(recordProcessorFactory).config(config).build()

    worker.run()
  }
}

