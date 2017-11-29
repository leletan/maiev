package org.leletan.maiev.job

import org.apache.spark.sql.SparkSession
/**
 * Created by jiale.tan on 4/26/17.
 */
object KafkaSourceTest extends App{
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
    .selectExpr("CAST(value AS STRING)")
    .as[String]

  // Generate running word count
  val wordCounts = lines.flatMap(_.split(" ")).groupBy("value").count()

  // Start running the query that prints the running counts to the console
  val query = wordCounts.writeStream
    .outputMode("complete")
    .format("console")
    .start()

  query.awaitTermination()
}
