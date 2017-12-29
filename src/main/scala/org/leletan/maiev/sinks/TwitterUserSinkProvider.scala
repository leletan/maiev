package org.leletan.maiev.sinks

import org.apache.spark.sql.{DataFrame, Dataset, SQLContext}
import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap
import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JdbcUtils}
import org.apache.spark.sql.execution.streaming.Sink
import org.apache.spark.sql.functions.from_json
import org.apache.spark.sql.sources.StreamSinkProvider
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.leletan.maiev.lib.{KafkaTopicData, ReliableKafkaUtils}

/**
 * Created by jiale.tan on 12/23/17.
 */
class JDBCUpsertSourceSink(sqlContext: SQLContext,
                           parameters: Map[String, String],
                           partitionColumns: Seq[String],
                           outputMode: OutputMode)
  extends ReliableKafkaUtils
    with Sink {

  val groupId: String = CaseInsensitiveMap(parameters)
    .getOrElse("kafka.group.id", {
      throw new IllegalArgumentException("'kafka.group.id' is not specified")
    })

  val jdbcOptions: Map[String, String] = CaseInsensitiveMap(parameters)
    .filter(_._1.startsWith("jdbc."))
    .map {
      case (k, v) =>
        k.stripPrefix("jdbc.") -> v
    }

  val options = new JDBCOptions(jdbcOptions)

  override def addBatch(batchId: Long, data: DataFrame): Unit = {
    processBatch(batchId, data, groupId) {
      ds: Dataset[KafkaTopicData] =>
        val schema =
          StructType(
            Seq(
              StructField("user", StructType(
                Seq(
                  StructField("id", IntegerType),
                  StructField("followers_count", IntegerType)
                )
              ))
            )
          )

        val getConnection = JdbcUtils.createConnectionFactory(options)

        import ds.sparkSession.implicits._
        ds.map(_.value)
          .select(from_json($"value", schema).as("data"))
          .select($"data.user.id".as("id"), $"data.user.followers_count".as("followers_count"))
          .filter("id is not null")
          .distinct()
          .foreachPartition {
            prt =>
              val conn = getConnection()
              val stmt = conn.createStatement()
              prt
                .grouped(100)
                .foreach {
                  group =>
                    val query =
                      "UPSERT INTO twitter.user VALUES" +
                        group.map {
                          row =>
                            "(" + row.getAs[Int]("id") + "," + row.getAs[Int]("followers_count") + ")"
                        }.mkString(", ") +
                        ";"
                    stmt.execute(query)
                }
              conn.close()
          }
    }
  }
}

class TwitterUserSinkProvider
  extends StreamSinkProvider {
  override def createSink(sqlContext: SQLContext,
                          parameters: Map[String, String],
                          partitionColumns: Seq[String],
                          outputMode: OutputMode): JDBCUpsertSourceSink = {

    new JDBCUpsertSourceSink(sqlContext, parameters, partitionColumns, outputMode)
  }
}