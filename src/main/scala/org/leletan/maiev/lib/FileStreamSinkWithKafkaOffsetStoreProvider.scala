package org.leletan.maiev.lib

import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap
import org.apache.spark.sql.execution.datasources.parquet.ParquetFileFormat
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.sources.StreamSinkProvider
import org.apache.spark.sql.streaming.OutputMode

/**
 * Created by jiale.tan on 12/6/17.
 */
class FileStreamSinkWithKafkaOffsetStoreProvider
  extends StreamSinkProvider {
  override def createSink(sqlContext: SQLContext,
                          parameters: Map[String, String],
                          partitionColumns: Seq[String],
                          outputMode: OutputMode): FileStreamSinkWithKafkaOffsetStore = {

    val filePath = CaseInsensitiveMap(parameters)
      .getOrElse("path", {
      throw new IllegalArgumentException("'path' is not specified")
    })

    val clientId = CaseInsensitiveMap(parameters)
      .getOrElse("group.id", {
        throw new IllegalArgumentException("'group.id' is not specified")
      })

    new FileStreamSinkWithKafkaOffsetStore(
      sparkSession = sqlContext.sparkSession,
      path = filePath,
      fileFormat = new ParquetFileFormat,
      partitionColumnNames = partitionColumns,
      options = parameters,
      offsetStore = KafkaOffsetStoreFactory.getKafkaOffsetStore,
      clientId = clientId
    )

  }

}
