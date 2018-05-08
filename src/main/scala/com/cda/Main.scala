package com.cda

import java.net.InetAddress
import java.util.UUID

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.cda.metrics.AuthAnalyzer
import com.cda.records.RecordProcessorFactory
import org.slf4j.LoggerFactory


object Main {

  val logger = LoggerFactory.getLogger(getClass)

  val credentialsProvider = new ProfileCredentialsProvider("cda")

  // Verify we can obtain credentials
  try credentialsProvider.getCredentials catch {
    case thr: Throwable =>
      logger.error(asString(thr))
      sys.exit(1)
  }

  val workerId = InetAddress.getLocalHost.getCanonicalHostName + ":" + UUID.randomUUID

  val config =
    new KinesisClientLibConfiguration(
      "auth-stream-processor",
      "devLogStream",
      credentialsProvider,
      workerId)

  def main(args: Array[String]): Unit = {
    // Ensure the JVM will refresh the cached IP values of AWS resources (e.g. service endpoints).
    java.security.Security.setProperty("networkaddress.cache.ttl", "60")

    AuthAnalyzer.run()

    val worker = new Worker.Builder().recordProcessorFactory(RecordProcessorFactory).config(config).build()

    worker.run()
  }
}

