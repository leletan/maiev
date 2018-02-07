package org.leletan.maiev.config

import org.leletan.maiev.job.RedshiftToRedis.safeGetConfigInt
import redis.clients.jedis.{JedisPool, JedisPoolConfig}


/**
 * Created by jiale.tan on 1/30/18.
 */
trait RedisClientConfig {
  this: SafeConfig =>
  lazy val redisHost: String = safeGetConfig("redis.host")
  lazy val redisPort: Int = safeGetConfigInt("redis.port")
  lazy val redisAuth: Option[String] = {
    val auth = safeGetConfig("redis.auth")
    if (auth.isEmpty) None
    else Some(auth)
  }

  lazy val redisDB: Int = safeGetConfigInt("redis.db")
  lazy val redisMaxConn: Int = safeGetConfigInt("redis.max.conn")
  lazy val maxRatePerPool: Int = safeGetConfigInt("redis.max.rate.per.pool")

}
