package org.leletan.maiev.job


import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import org.leletan.maiev.config.{AwsConfig, JDBCConfig, KafkaConfig, SafeConfig}
import org.leletan.maiev.lib.ReliableKafkaUtils

/**
 * Created by jiale.tan on 4/26/17.
 */
object KafkaSourceTest
  extends App
    with ReliableKafkaUtils
    with KafkaConfig
    with AwsConfig
    with JDBCConfig
    with SafeConfig {

  val defaultConfigFileName = "KafkaSourceTest"

  override def config: Config = {
    val confKey = "SPARK_CONFIG_FILE"
    val fileName = System.getProperty(confKey, Option(System.getenv(confKey)).getOrElse(defaultConfigFileName))
    ConfigFactory.load(s"$fileName.conf")
  }

  val spark = SparkSession
    .builder
    .appName("StructuredKafkaWordCount")
    .getOrCreate()


  val lines = createStreamFromOffsets(
    spark,
    topics,
    groupId,
    brokers,
    maxOffsetsPerTrigger)
    .repartition(2)

  lines
    .writeStream
    .format("org.leletan.maiev.sinks.ReliableParquetSinkProvider")
    .outputMode("append")
    .trigger(Trigger.ProcessingTime("10 seconds"))
    .option("checkpointLocation", "/tmp/checkpoint/parquet")
    .option("kafka.group.id", groupId)
    .option("path", s"s3a://$s3Bucket/$s3Prefix")
    .start()

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

  lines
    .writeStream
    .format("console")
    .outputMode("append")
    .trigger(Trigger.ProcessingTime("25 seconds"))
    .start()

  spark.streams.awaitAnyTermination()

}

