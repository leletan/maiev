package org.leletan.maiev.job

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel
import org.joda.time.DateTime
import org.leletan.maiev.config._
import org.leletan.maiev.lib.{JedisFactory, Logger, StatsClient}
import redis.clients.jedis.Jedis

import scala.util.{Failure, Success, Try}

/**
 * Created by jiale.tan on 1/30/18.
 */
object RedshiftToRedis
  extends App
    with JobConfig
    with AwsConfig
    with RedisClientConfig
    with RedshiftConfig
    with SafeConfig
    with Logger {

  val defaultConfigFileName = "RedshiftToRedis"
  val start = safeGetConfig("spark.redshift2redis.start")
  val end = safeGetConfig("spark.redshift2redis.end")

  override def config: Config = {
    val confKey = "SPARK_CONFIG_FILE"
    val fileName = System.getProperty(confKey, Option(System.getenv(confKey)).getOrElse(defaultConfigFileName))
    ConfigFactory.load(s"$fileName.conf")
  }

  val spark = SparkSession
    .builder
    .appName("RedshiftToRedis")
    .getOrCreate()

  spark.sparkContext.setLogLevel(jobLogLevel)


  val query =
    "SELECT dev_id, " +
      "advertiser_app_store_id, " +
      "DATEDIFF(SECONDS, install_postbacks.timestamp, getdate()) AS seconds_till_now " +
      "FROM public.install_postbacks " +
      s"WHERE timestamp >= '$start' " +
      s"AND timestamp < '$end' " +
      s"AND dev_id IS NOT NULL " +
      s"AND advertiser_app_store_id IS NOT NULL "

  import spark.implicits._

  val installs = spark
    .read
    .format("com.databricks.spark.redshift")
    .option("url", redshiftJDBCURL)
    .option("forward_spark_s3_credentials", "true")
    .option("tempdir", tempS3Dir)
    .option("query", query)
    .load()
    .as[InstallHisotry]
    .persist(StorageLevel.MEMORY_AND_DISK_SER_2)

  installs.show()

  lazy val maxTryCnt = safeGetConfigInt("redshift2redis.retry.cnt")
  lazy val bfSha = safeGetConfig("redshift2redis.bf.sha")

  installs.foreachPartition {
    partition =>
      val bfErrRate = "0.01"
      val bfReservedCap = "1"
      val conn = JedisFactory.getConn
      partition.foreach {
        install =>
          StatsClient.Incr("tx.attempt")
          retry[RedisResponse](maxTryCnt)(upsertInstallHistory(install, conn, bfSha, bfErrRate, bfReservedCap)) match {
            case Success(s) =>
              StatsClient.Incr("tx.succeeded")
            case Failure(e) =>
              error(e)
              info(s"upsertInstallHistory($install, conn, $bfSha, $bfErrRate, $bfReservedCap)")
              StatsClient.Incr("tx.failed")
          }
      }
      conn.close()
  }

  @annotation.tailrec
  def retry[T](n: Int)(fn: => Try[T]): Try[T] = {
    fn match {
      case x: util.Success[T] => x
      case _ if n > 1 =>
        Thread.sleep(50)
        retry(n - 1)(fn)
      case f => f
    }
  }

  def upsertInstallHistory(install: InstallHisotry,
                           conn: Jedis,
                           sha: String,
                           bfErrRate: String,
                           bfReservedCap: String): Try[RedisResponse] = {
    val key = s"sp:1:${install.dev_id}"
    val value = install.advertiser_app_store_id
    val potentialTTL = s"${31536000 - install.seconds_till_now}"

    for {
      res <- Try {
        val startTime = DateTime.now().getMillis
        val resp = conn
          .evalsha(sha, 5, key, value, potentialTTL, bfErrRate, bfReservedCap)
          .asInstanceOf[java.util.ArrayList[Any]]
          .toArray()
          .toSeq
        StatsClient.reportExecutionTime("bf_insert.duration", DateTime.now().getMillis - startTime)
        resp
      } match {
        case Success(s) =>
          StatsClient.Incr("result_array.succeeded")
          Success(s)
        case Failure(e) =>
          StatsClient.Incr("result_array.failed")
          error(e)
          info(s"conn.evalsha($sha, 5, $key, $value, $potentialTTL, $bfErrRate, $bfReservedCap)")
          Failure(e)
      }

      bfReserve <- Try(
        res.head.asInstanceOf[String].trim
      ) match {
        case Success(s) =>
          if (s == "OK") StatsClient.Incr("bf_reserve.succeeded")
          else if (s == "NOT_CALLED") StatsClient.Incr("bf_reserve.not_call")
          else StatsClient.Incr("bf_reserve.other")
          Success(s)
        case Failure(e) =>
          StatsClient.Incr("bf_reserve.failed")
          error(e)
          Failure(e)
      }

      bfInsert <- Try(
        res(1).asInstanceOf[Long]
      ) match {
        case Success(s) =>
          if (s == 1) StatsClient.Incr("bf_insert.succeeded")
          else if (s == 0) StatsClient.Incr("bf_insert.dup")
          else StatsClient.Incr("bf_insert.other")
          Success(s)
        case Failure(e) =>
          StatsClient.Incr("bf_insert.failed")
          error(e)
          Failure(e)
      }

      keyTTL <- Try(
        res(2).asInstanceOf[Long]
      ) match {
        case Success(s) =>
          if (s == 1) StatsClient.Incr("ttl.succeeded")
          else if (s == 0) StatsClient.Incr("ttl.bigger_value_set")
          else StatsClient.Incr("ttl.other")
          Success(s)
        case Failure(e) =>
          StatsClient.Incr("ttl.failed")
          error(e)
          Failure(e)
      }

    } yield {
      RedisResponse(bfReserve, bfInsert, keyTTL)
    }
  }
}

case class InstallHisotry(dev_id: String,
                          advertiser_app_store_id: String,
                          seconds_till_now: Long)

case class RedisResponse(reserve: String,
                         insert: Long,
                         ttl: Long)



