package org.leletan.maiev.job


import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap
import org.apache.spark.sql.functions._
import org.apache.spark.sql.sources.StreamSinkProvider
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types._
import org.leletan.maiev.config.{AwsConfig, KafkaConfig, SafeConfig}
import org.leletan.maiev.lib.{JDBCConnectionFactory, KafkaOffsetStoreFactory, KafkaTopicData, KafkaUtilities}
import org.leletan.maiev.sinks.ReliableKafkaSink

/**
 * Created by jiale.tan on 4/26/17.
 */
object KafkaSourceTest
  extends App
    with KafkaUtilities
    with KafkaConfig
    with AwsConfig
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


  val lines = createStreamFromOffsets(spark,
    topics,
    groupId,
    brokers,
    maxOffsetsPerTrigger)

  //  lines
  //    .writeStream
  //    .outputMode("append")
  //    .format("org.leletan.maiev.sinks.FileStreamSinkWithKafkaOffsetStoreProvider")
  //    .option("checkpointLocation", "/tmp/checkpoint")
  //    .trigger(Trigger.ProcessingTime("10 seconds"))
  //    .option("path", s"s3a://$s3Bucket/$s3Prefix")
  //    .option("group.id", groupId)
  //    .start()

  lines
    .writeStream
    .format("org.leletan.maiev.sinks.TwitterUserSinkProvider")
    .outputMode("update")
    .trigger(Trigger.ProcessingTime("25 seconds"))
    .option("group.id", groupId)
    .option("checkpointLocation", "/tmp/checkpoint")
    .start()

  //  output
  //      .writeStream
  //      .format("console")
  //      .outputMode("append")
  //      .trigger(Trigger.ProcessingTime("25 seconds"))
  //      .start()

  spark.streams.awaitAnyTermination()

}

