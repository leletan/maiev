package org.leletan.maiev.job

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import org.joda.time.DateTime
import org.leletan.maiev.config._
import org.leletan.maiev.lib.ReliableKafkaUtils

/**
 * Created by jiale.tan on 4/26/17.
 */
object ReliableS3ParquetLogging
  extends App
    with ReliableKafkaUtils
    with KafkaConfig
    with AwsConfig
    with JDBCConfig
    with JobConfig
    with SafeConfig {

  val defaultConfigFileName = "ReiliableS3ParquetLogging"

  override def config: Config = {
    val confKey = "SPARK_CONFIG_FILE"
    val fileName = System.getProperty(confKey, Option(System.getenv(confKey)).getOrElse(defaultConfigFileName))
    ConfigFactory.load(s"$fileName.conf")
  }

  val spark = SparkSession
    .builder
    .appName("ReiliableS3ParquetLogging")
    .getOrCreate()

  spark.sparkContext.setLogLevel(jobLogLevel)

  val lines = createStreamFromOffsets(
    spark,
    topics,
    groupId,
    brokers,
    maxOffsetsPerTrigger)

  lines
    .writeStream
    .format("org.leletan.maiev.sinks.ReliableParquetSinkProvider")
    .outputMode("append")
    .trigger(Trigger.ProcessingTime("60 seconds"))
    .option("checkpointLocation", s"/tmp/checkpoint/parquet/")
    .option("kafka.group.id", groupId)
    .option("path", s"s3a://$s3Bucket/$s3Prefix")
    .start()

  spark.streams.awaitAnyTermination()

}

