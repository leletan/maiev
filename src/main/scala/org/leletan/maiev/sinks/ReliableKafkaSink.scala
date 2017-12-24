package org.leletan.maiev.sinks

import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.execution.streaming.Sink
import org.apache.spark.sql.functions.{max, min}
import org.apache.spark.sql.types.LongType
import org.apache.spark.storage.StorageLevel
import org.leletan.maiev.lib._

/**
 * ReliableKafkaSink should only be directly reading from kafka dataframe
 * Or at least make sure the topic, partition and offset columns are in the dataframe
 * Created by jiale.tan on 12/21/17.
 */
trait ReliableKafkaSink
  extends Sink
    with KafkaUtilities
    with Logger {

  /**
   * addBatch will record the offset, process the data
   * and verify and save the offset in the store for every each mini batch
   * @param batchId
   * @param data
   */
  override def addBatch(batchId: Long, data: DataFrame): Unit = {
    data.persist(StorageLevel.MEMORY_AND_DISK_SER)

    import data.sparkSession.implicits._
    val allKafkaMetaData = data
      .groupBy($"topic", $"partition")
      .agg(max($"offset").cast(LongType).as("to"), min($"offset").cast(LongType).as("from"))
      .as[KafkaMetadata]
      .collect()

    process(data.selectExpr("topic", "CAST(value AS STRING)").as[KafkaTopicData])

    data.unpersist()

    try {
      verifyAndStoreOffset(getGroupId, KafkaOffsetStoreFactory.getKafkaOffsetStore, allKafkaMetaData)
    } catch {
      case e: Exception =>
        error(e)
        // Exit the application to prevent offset gaps, otherwise it may result in
        // the application keeps running but not updates offsets in storage.
        sys.exit(1)
    }

  }

  /**
   * get the group id of the kafka consumer
   * @return
   */
  def getGroupId: String

  /**
   * process the data
   * @return
   */
  def process: Dataset[KafkaTopicData] => Unit

}
