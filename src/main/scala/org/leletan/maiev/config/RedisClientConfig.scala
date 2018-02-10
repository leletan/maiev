package org.leletan.maiev.config

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
  lazy val redisMaxRatePerPool: Int = safeGetConfigInt("redis.max.rate.per.pool")
  lazy val redisMaxConnPerPool: Int = safeGetConfigInt("redis.max.conn.per.pool")
  lazy val redisMaxIdlePerPool: Int = safeGetConfigInt("redis.max.idel.per.pool")
  lazy val redisTimeoutInMillis: Int = safeGetConfigInt("redis.timeout.in.millis")

}
