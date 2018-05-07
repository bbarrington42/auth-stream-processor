package com.cda

import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{KinesisClientLibConfiguration, Worker}
import com.cda.records.RecordProcessorFactory

object Main {

  val config: KinesisClientLibConfiguration = ???

  def main(args: Array[String]): Unit = {

    val worker = new Worker.Builder().recordProcessorFactory(RecordProcessorFactory).config(config).build()

    worker.run()
  }
}
