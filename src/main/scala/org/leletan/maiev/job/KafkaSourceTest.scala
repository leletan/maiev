package org.leletan.maiev.job

import java.sql.Timestamp

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import org.leletan.maiev.config.{AwsConfig, SafeConfig}
/**
 * Created by jiale.tan on 4/26/17.
 */
object KafkaSourceTest
  extends App
  with AwsConfig
  with SafeConfig
{

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

  import spark.implicits._

  // Create DataSet representing the stream of input lines from kafka
  val lines = spark
    .readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "kafka:9092")
    .option("subscribe", "topic1")
    .option("startingOffsets", "latest")
    .option("failOnDataLoss", "true")
    .load()
    .selectExpr("CAST(value AS STRING)", "topic", "partition", "offset", "timestamp")
    .as[(String, String, Int, Long, Timestamp)]

  val query = lines
    .writeStream
    .outputMode("append")
    .format("parquet")
    .partitionBy("timestamp")
    .option("checkpointLocation", "/tmp/checkpoint")
    .trigger(Trigger.ProcessingTime("10 seconds"))
    .start(s"s3a://$s3Bucket/$s3Prefix/parquest_test")

  query.awaitTermination()
}
