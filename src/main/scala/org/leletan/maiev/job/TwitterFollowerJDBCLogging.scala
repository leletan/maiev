package org.leletan.maiev.job

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import org.leletan.maiev.config._
import org.leletan.maiev.job.ReliableS3ParquetLogging.jobLogLevel
import org.leletan.maiev.lib.ReliableKafkaUtils

/**
 * Created by jiale.tan on 4/26/17.
 */
object TwitterFollowerJDBCLogging
  extends App
    with ReliableKafkaUtils
    with KafkaConfig
    with AwsConfig
    with JDBCConfig
    with JobConfig
    with SafeConfig {

  val defaultConfigFileName = "TwitterFollwerJDBCLogging"

  override def config: Config = {
    val confKey = "SPARK_CONFIG_FILE"
    val fileName = System.getProperty(confKey, Option(System.getenv(confKey)).getOrElse(defaultConfigFileName))
    ConfigFactory.load(s"$fileName.conf")
  }

  val spark = SparkSession
    .builder
    .appName("TwitterFollwerJDBCLogging")
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
    .format("org.leletan.maiev.sinks.TwitterUserSinkProvider")
    .outputMode("update")
    .trigger(Trigger.ProcessingTime("25 seconds"))
    .option("checkpointLocation", "/tmp/checkpoint/jdbc")
    .option("kafka.group.id", groupId)
    .option("jdbc.driver", jdbcDriver)
    .option("jdbc.url", jdbcURL)
    .option("jdbc.user", jdbcUser)
    .option("jdbc.password", jdbcPassword)
    .option("jdbc.dbtable", "twitter.user")
    .start()

  spark.streams.awaitAnyTermination()

}

