package com.cda

import java.net.InetAddress
import java.util.UUID

import com.amazonaws.auth.{AWSCredentialsProvider, InstanceProfileCredentialsProvider}
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.{IRecordProcessor, IRecordProcessorFactory}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.cda.metrics.AuthAnalyzer
import com.cda.records.RecordProcessor
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._


object Main {

  def createWorker(env: String): Worker = {
    val workerId = InetAddress.getLocalHost.getCanonicalHostName + ":" + UUID.randomUUID
    val recordProcessorFactory = new IRecordProcessorFactory {
      override def createProcessor(): IRecordProcessor = new RecordProcessor(new AuthAnalyzer(env))
    }
    val config =
      new KinesisClientLibConfiguration(
        "auth-stream-processor-" + env,
        env + "LogStream",
        credentialsProvider,
        workerId)
    new Worker.Builder().recordProcessorFactory(recordProcessorFactory).config(config).build()
  }

  val logger = LoggerFactory.getLogger(getClass)

  val config = ConfigFactory.load()

  // The role associated with the EC2 instance will provide our credentials
  val credentialsProvider = InstanceProfileCredentialsProvider.getInstance()


  def main(args: Array[String]): Unit = {
    // Ensure the JVM will refresh the cached IP values of AWS resources.
    java.security.Security.setProperty("networkaddress.cache.ttl", "60")

    val appConfig = config.getConfig("app")
    val envs = appConfig.getStringList("env").asScala

    // App designed to handle more than one environment. Create a Worker for each.
    val workers = envs.map(env => createWorker(env))

    workers.foreach(_.run())
  }
}

