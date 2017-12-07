package org.leletan.maiev.job

import java.sql.Timestamp

import com.twitter.zk.ZkClient
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import org.leletan.maiev.config.{AwsConfig, KafkaConfig, SafeConfig}
import org.leletan.maiev.lib.{KafkaUnloader, ZookeeperKafkaOffsetStore}

/**
 * Created by jiale.tan on 4/26/17.
 */
object KafkaSourceTest
  extends App
    with KafkaUnloader
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

  val lines = createStreamFromOffsets(spark)

  lines
    .writeStream
    .outputMode("append")
    .format("org.leletan.maiev.lib.FileStreamSinkWithKafkaOffsetStoreProvider")
    .option("checkpointLocation", "/tmp/checkpoint")
    .trigger(Trigger.ProcessingTime("10 seconds"))
    .option("path", s"s3a://$s3Bucket/$s3Prefix/parquest_test")
    .option("group.id", groupId)
    .start()

  spark.streams.awaitAnyTermination()
}
