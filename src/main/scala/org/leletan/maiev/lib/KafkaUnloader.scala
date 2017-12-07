package org.leletan.maiev.lib

import com.sun.jersey.json.impl.JSONHelper
import com.twitter.util.{Await, Future}
import org.apache.spark.sql.streaming.DataStreamReader
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.leletan.maiev.config.{KafkaConfig, SafeConfig}

/**
 * Created by jialeKafkaExtractor.tan on 12/5/17.
 */
trait KafkaUnloader extends Logger{
  this: SafeConfig with KafkaConfig =>

  def createStreamFromOffsets(spark: SparkSession): DataFrame = {
    val offsetStore = KafkaOffsetStoreFactory.getKafkaOffsetStore

    val fromOffsets =
      Await.result(
        Future.collect(
          topics.map {
            topic =>
              offsetStore.get(ClientIdAndTopic(groupId, topic))
                .map(_.getOrElse(Map()))
                .map {
                  _.map {
                    case (k, v) =>
                      (k.toString, v)
                  }
                }
                .map(topic -> _)
          }
        )
      )
        .filter(_._2.nonEmpty)
        .toMap


    val streamReader = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", brokers)
      .option("subscribe", topics.mkString(","))
      .option("failOnDataLoss", "true")
      .option("group.id", groupId)

    if (fromOffsets.nonEmpty) {

      info(s"startingOffsetsMap: $fromOffsets")

      val startingOffsets = JsonHelper.toJson(fromOffsets)

      info(s"startingOffsets: $startingOffsets")

      streamReader
        .option("startingOffsets", startingOffsets)
        .load()

    } else {
      streamReader
        .option("startingOffsets", "latest")
        .load()
    }
  }

}
