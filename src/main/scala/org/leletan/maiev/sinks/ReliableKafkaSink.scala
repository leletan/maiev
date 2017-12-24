package org.leletan.maiev.sinks

import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.execution.streaming.Sink
import org.apache.spark.sql.functions.{max, min}
import org.apache.spark.sql.types.LongType
import org.apache.spark.storage.StorageLevel
import org.leletan.maiev.lib._

/**
 * Created by jiale.tan on 12/21/17.
 */
class ReliableKafkaSink(
                         groupId: String,
                         process: (Dataset[KafkaTopicData] => Unit))
//url:String, user:String, pwd:String
  extends Sink
    with KafkaUtilities
    with Logger {

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
      verifyAndStoreOffset(groupId, KafkaOffsetStoreFactory.getKafkaOffsetStore, allKafkaMetaData)
    } catch {
      case e: Exception =>
        error(e)
        // Exit the application to prevent offset gaps, otherwise it may result in
        // the application keeps running but not updates offsets in storage.
        sys.exit(1)
    }


  }


  //  extends ForeachWriter[Row] {
  //  val driver = "org.postgresql.Driver"
  //  var connection:Connection = _
  //  var statement:Statement = _
  //
  //  def open(partitionId: Long,version: Long): Boolean = {
  //    Class.forName(driver)
  //    connection = DriverManager.getConnection("jdbc:postgresql://cockroachdb-public:26257/bank?sslmode=disable", "leletan", "leletan")
  //    statement = connection.createStatement
  //    true
  //  }
  //
  //  def process(value: Row): Unit = {
  //    statement.executeUpdate("UPSERT INTO twitter.user " +
  //      "VALUES (" + value.getAs[Int]("id") + "," + value.getAs[Int]("followers_count") + ")")
  //  }
  //
  //  def close(errorOrNull: Throwable): Unit = {
  //    connection.close()
  //  }
}
