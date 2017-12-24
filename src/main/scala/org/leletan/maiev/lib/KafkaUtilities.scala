package org.leletan.maiev.lib

import com.twitter.util.{Await, Future}
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
 * Created by jialeKafkaExtractor.tan on 12/5/17.
 */
trait KafkaUtilities extends Logger {

  def createStreamFromOffsets(spark: SparkSession,
                              topics: Array[String],
                              groupId: String,
                              brokers: String,
                              maxOffsetsPerTrigger: String): DataFrame = {
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
      .option("maxOffsetsPerTrigger", maxOffsetsPerTrigger)

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

  def verifyAndStoreOffset(clientId: String,
                           offsetStore: KafkaOffsetStore,
                           allKafkaMetaData: Array[KafkaMetadata]): Unit = {
    Await.result(
      Future.collect(
        allKafkaMetaData
          .groupBy(_.topic)
          .map {
            case (topic, kafkaMetadataList) =>
              // keep only offsets for the topic in consideration
              val offsetsToBeSaved = kafkaMetadataList
                .map(o => (o.partition, (o.from, o.to + 1)))
                .toMap

              val clientIdAndTopic = ClientIdAndTopic(clientId, topic)
              // before saving, check if the offsets are consistent with the ones previously saved
              offsetStore.get(clientIdAndTopic).flatMap { savedOffsetsOpt =>
                savedOffsetsOpt.foreach { savedOffsets =>
                  val missingPartitions = savedOffsets.keySet.diff(offsetsToBeSaved.keySet)
                  if (missingPartitions.nonEmpty) {
                    throw new IllegalStateException(
                      s"missing previously saved partitions ${missingPartitions.mkString(", ")}"
                    )
                  }

                  val partitionsWithGaps = offsetsToBeSaved.filter { case (p, (fo, _)) =>
                    savedOffsets.contains(p) && savedOffsets(p) != fo
                  }.keySet
                  if (partitionsWithGaps.nonEmpty) {
                    throw new IllegalStateException(
                      s"found offset gap in partitions ${partitionsWithGaps.mkString(", ")}"
                    )
                  }
                }
                offsetStore.put(clientIdAndTopic, Some(offsetsToBeSaved.mapValues(_._2)))
              }
          }.toSeq
      )
    )
  }
}
