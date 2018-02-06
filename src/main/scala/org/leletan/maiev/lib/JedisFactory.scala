package org.leletan.maiev.lib

import org.leletan.maiev.config.{RedisClientConfig, SafeConfig}
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

/**
 * Created by jiale.tan on 1/31/18.
 */
@transient
object JedisFactory
  extends RedisClientConfig
    with SafeConfig
    with Logger {

  lazy private val jedisPool = {

    info(s"jedis pool config: redisHost: $redisHost, redisPort: $redisPort, redisAuth: $redisAuth, redisDB: $redisDB")

    new JedisPool(
      {
        val pc = new JedisPoolConfig()
        pc.setMaxTotal(128)
        pc
      },
      redisHost,
      redisPort,
      5000,
      redisAuth.orNull,
      redisDB
    )
  }

  def getConn: Jedis = {
    jedisPool.getResource
  }

}

// import org.spark_project.guava.util.concurrent.RateLimiter
// import scala.concurrent.duration.Duration
// import scala.concurrent.{Await, Future}
//object test extends App {
//
//  lazy val redisAuth: String = {
//    val auth = ""
//    if (auth.isEmpty) {
//      null
//    } else
//      auth
//  }
//
//  val jedisPool = new JedisPool(
//    {
//      val pc = new JedisPoolConfig()
//      pc.setMaxTotal(128)
//      pc
//    },
//    "xxx.yyy.zzz.aa",
//    6379,
//    5000,
//    null,
//    0
//  )
//
//  val rateLimiter = RateLimiter.create(3.0)
//
//  import scala.concurrent.ExecutionContext.Implicits.global
//
//  Await.result(
//  Future.sequence(
//  (1 to 10).map{
//  i =>
//    Future[Unit] {
//      rateLimiter.acquire()
//      println(s"thread $i: ${jedisPool.getResource.getDB}")
//    }
//  }), Duration.Inf)
//
//}