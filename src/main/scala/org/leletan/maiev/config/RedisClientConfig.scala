package org.leletan.maiev.config

import redis.clients.jedis.{JedisPool, JedisPoolConfig}


/**
 * Created by jiale.tan on 1/30/18.
 */
trait RedisClientConfig {
  this: SafeConfig =>
  lazy val redisHost: String = safeGetConfig("redis.host")
  lazy val redisPort: Int = safeGetConfigInt("redis.port")
  lazy val redisAuth: String = safeGetConfig("redis.auth")
  lazy val redisDB: Int = safeGetConfigInt("redis.db")
  lazy val redisMaxConn: Int = safeGetConfigInt("redis.max.conn")
}
