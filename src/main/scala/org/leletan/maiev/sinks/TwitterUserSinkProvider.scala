package org.leletan.maiev.sinks

import java.sql.{Connection, DriverManager, Statement}

import org.apache.spark.sql.{Dataset, SQLContext}
import org.apache.spark.sql.catalyst.util.CaseInsensitiveMap
import org.apache.spark.sql.functions.from_json
import org.apache.spark.sql.sources.StreamSinkProvider
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.leletan.maiev.lib.{JDBCConnectionFactory, KafkaTopicData}

/**
 * Created by jiale.tan on 12/23/17.
 */
class TwitterUserSink(groupId: String,
                      jdbcDriver: String,
                      jdbcURL: String,
                      jdbcUser: String,
                      jdbcPassword: String)
  extends ReliableKafkaSink {

  override def getGroupId: String = groupId

  override def process: Dataset[KafkaTopicData] => Unit = {
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

      import ds.sparkSession.implicits._
      ds.map(_.value)
        .select(from_json($"value", schema).as("data"))
        .select($"data.user.id".as("id"), $"data.user.followers_count".as("followers_count"))
        .filter("id is not null")
        .foreachPartition {
          prt =>
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
                  JDBCConnectionFactory.connection.createStatement().execute(query)
              }
        }
  }
}

class TwitterUserSinkProvider
  extends StreamSinkProvider {
  override def createSink(sqlContext: SQLContext,
                          parameters: Map[String, String],
                          partitionColumns: Seq[String],
                          outputMode: OutputMode): ReliableKafkaSink = {

    val clientId = CaseInsensitiveMap(parameters)
      .getOrElse("group.id", {
        throw new IllegalArgumentException("'group.id' is not specified")
      })

    val jdbcDriver = CaseInsensitiveMap(parameters)
      .getOrElse("jdbc.driver", {
        throw new IllegalArgumentException("'jdbc.driver' is not specified")
      })

    val jdbcURL = CaseInsensitiveMap(parameters)
      .getOrElse("jdbc.url", {
        throw new IllegalArgumentException("'jdbc.url' is not specified")
      })

    val jdbcUser = CaseInsensitiveMap(parameters)
      .getOrElse("jdbc.user", {
        throw new IllegalArgumentException("'jdbc.user' is not specified")
      })

    val jdbcPassword = CaseInsensitiveMap(parameters)
      .getOrElse("jdbc.password", {
        throw new IllegalArgumentException("'jdbc.password' is not specified")
      })

    new TwitterUserSink(
      groupId = clientId,
      jdbcDriver = jdbcDriver,
      jdbcURL = jdbcURL,
      jdbcUser = jdbcUser,
      jdbcPassword = jdbcPassword
    )
  }
}