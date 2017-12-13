package org.leletan.maiev.lib

import com.twitter.util.{Await, Future}
import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.execution.datasources.FileFormat
import org.apache.spark.sql.execution.streaming.FileStreamSink
import org.apache.spark.sql.functions.{max, min}
import org.apache.spark.sql.types.LongType

/**
 * Created by jiale.tan on 12/4/17.
 */
class FileStreamSinkWithKafkaOffsetStore(
                                          sparkSession: SparkSession,
                                          path: String,
                                          fileFormat: FileFormat,
                                          partitionColumnNames: Seq[String],
                                          options: Map[String, String],
                                          offsetStore: KafkaOffsetStore,
                                          clientId: String
                                        )
  extends FileStreamSink(
    sparkSession: SparkSession,
    path: String,
    fileFormat: FileFormat,
    partitionColumnNames: Seq[String],
    options: Map[String, String])
    with Logger {



  override def addBatch(batchId: Long, data: DataFrame): Unit = {
    import data.sparkSession.implicits._
    val allKafkaMetaData = data
      .groupBy($"topic", $"partition")
      .agg(max($"offset").cast(LongType).as("to"), min($"offset").cast(LongType).as("from"))
      .as[KafkaMetadata]
      .collect()

    super.addBatch(batchId, data)

    try {
      storeOffset(clientId, allKafkaMetaData)
    } catch {
      case e: Exception =>
        error(e)
        // Exit the application to prevent offset gaps, otherwise it may result in
        // the application keeps running but not updates offsets in storage.
        sys.exit(1)
    }

  }

  private def storeOffset(clientId: String,
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
