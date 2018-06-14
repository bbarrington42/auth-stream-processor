package com.cda

import java.net.InetAddress
import java.util.UUID

import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.cda.metrics.AuthAnalyzer
import com.cda.records.RecordProcessor
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._


object Main {


  val logger = LoggerFactory.getLogger(getClass)

  val config = ConfigFactory.load()

  // The role associated with the EC2 instance will provide our credentials
  val credentialsProvider = InstanceProfileCredentialsProvider.getInstance()

  val kinesisStreamPrefix = "consumer-logs-kinesis-stream-"


  def createWorker(env: String): Worker = {
    val workerId = InetAddress.getLocalHost.getCanonicalHostName + ":" + UUID.randomUUID

    val recordProcessorFactory = new IRecordProcessorFactory {
      override def createProcessor(): IRecordProcessor = {
        logger.debug(s"Creating RecordProcessor for env $env")
        new RecordProcessor(AuthAnalyzer(env))
      }
    }

    val config =
      new KinesisClientLibConfiguration(
        "auth-stream-processor-" + env,
        kinesisStreamPrefix + env,
        credentialsProvider,
        workerId)
    logger.info(s"Creating Worker for env $env with workerId $workerId")
    new Worker.Builder().recordProcessorFactory(recordProcessorFactory).config(config).build()
  }


  def main(args: Array[String]): Unit = try {
    // Ensure the JVM will refresh the cached IP values of AWS resources.
    java.security.Security.setProperty("networkaddress.cache.ttl", "60")

    val appConfig = config.getConfig("app")
    val envs = appConfig.getStringList("env").asScala

    
    // App designed to handle more than one environment. Create a Worker for each.
    val workers = envs.map(env => createWorker(env))

    // Each Worker needs to run in its own thread.
    workers.foreach(worker => new Thread(worker).start)
    synchronized {
      wait() // Suspend forever
    }
  } catch {
    case t: Throwable =>
      logger.error(asString(t))
  }
}

