package org.leletan.maiev.sinks

import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap
import org.apache.spark.sql.execution.datasources.FileFormat
import org.apache.spark.sql.execution.datasources.parquet.ParquetFileFormat
import org.apache.spark.sql.execution.streaming.FileStreamSink
import org.apache.spark.sql.sources.StreamSinkProvider
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.{DataFrame, Dataset, SQLContext, SparkSession}
import org.leletan.maiev.lib._

/**
 * Created by jiale.tan on 12/4/17.
 */
class ReliableParquetSink(
                           sparkSession: SparkSession,
                           path: String,
                           fileFormat: FileFormat,
                           partitionColumnNames: Seq[String],
                           parameters: Map[String, String])
  extends FileStreamSink(
    sparkSession: SparkSession,
    path: String,
    fileFormat: FileFormat,
    partitionColumnNames: Seq[String],
    parameters: Map[String, String])
    with ReliableKafkaUtils
    with Logger {

  val groupId: String = CaseInsensitiveMap(parameters)
    .getOrElse("kafka.group.id", {
      throw new IllegalArgumentException("'kafka.group.id' is not specified")
    })

  override def addBatch(batchId: Long, data: DataFrame): Unit = {
    processBatch(batchId, data, groupId){
      ds: Dataset[KafkaTopicData] =>
        super.addBatch(batchId, ds.toDF())
    }
  }
}


class ReliableParquetSinkProvider
  extends StreamSinkProvider {
  override def createSink(sqlContext: SQLContext,
                          parameters: Map[String, String],
                          partitionColumns: Seq[String],
                          outputMode: OutputMode): ReliableParquetSink = {

    val filePath = CaseInsensitiveMap(parameters)
      .getOrElse("path", {
        throw new IllegalArgumentException("'path' is not specified")
      })

    new ReliableParquetSink(
      sparkSession = sqlContext.sparkSession,
      path = filePath,
      fileFormat = new ParquetFileFormat,
      partitionColumnNames = partitionColumns,
      parameters = parameters
    )
  }
}