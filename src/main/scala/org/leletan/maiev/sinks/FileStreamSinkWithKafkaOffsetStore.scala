//package org.leletan.maiev.sinks
//
//import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap
//import org.apache.spark.sql.execution.datasources.FileFormat
//import org.apache.spark.sql.execution.datasources.parquet.ParquetFileFormat
//import org.apache.spark.sql.execution.streaming.FileStreamSink
//import org.apache.spark.sql.sources.StreamSinkProvider
//import org.apache.spark.sql.streaming.OutputMode
//import org.apache.spark.sql.{DataFrame, Dataset, SQLContext, SparkSession}
//import org.leletan.maiev.lib._
//
///**
// * Created by jiale.tan on 12/4/17.
// */
//class ReliableParquetSink(
//                           sparkSession: SparkSession,
//                           path: String,
//                           fileFormat: FileFormat,
//                           partitionColumnNames: Seq[String],
//                           options: Map[String, String],
//                           offsetStore: KafkaOffsetStore,
//                           clientId: String
//                         )
//  extends FileStreamSink (
//    sparkSession: SparkSession,
//    path: String,
//    fileFormat: FileFormat,
//    partitionColumnNames: Seq[String],
//    options: Map[String, String])
//    with ReliableKafkaSink
//    with Logger {
//
//  override def getGroupId: String = clientId
//
//  override def process: Dataset[KafkaTopicData] => Unit = {
//  }
//}
//
//class FileStreamSinkWithKafkaOffsetStoreProvider
//  extends StreamSinkProvider {
//  override def createSink(sqlContext: SQLContext,
//                          parameters: Map[String, String],
//                          partitionColumns: Seq[String],
//                          outputMode: OutputMode): FileStreamSinkWithKafkaOffsetStore = {
//
//    val filePath = CaseInsensitiveMap(parameters)
//      .getOrElse("path", {
//        throw new IllegalArgumentException("'path' is not specified")
//      })
//
//    val clientId = CaseInsensitiveMap(parameters)
//      .getOrElse("group.id", {
//        throw new IllegalArgumentException("'group.id' is not specified")
//      })
//
//    new FileStreamSinkWithKafkaOffsetStore(
//      sparkSession = sqlContext.sparkSession,
//      path = filePath,
//      fileFormat = new ParquetFileFormat,
//      partitionColumnNames = partitionColumns,
//      options = parameters,
//      offsetStore = KafkaOffsetStoreFactory.getKafkaOffsetStore,
//      clientId = clientId
//    )
//  }
//}