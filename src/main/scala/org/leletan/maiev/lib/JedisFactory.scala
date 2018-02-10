package org.leletan.maiev.lib

import org.leletan.maiev.config.{RedisClientConfig, SafeConfig}
import org.spark_project.guava.util.concurrent.RateLimiter
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

/**
 * Created by jiale.tan on 1/31/18.
 */
@transient
object JedisFactory
  extends RedisClientConfig
    with SafeConfig
    with Logger {

  lazy val rateLimiter: RateLimiter = RateLimiter.create(redisMaxRatePerPool.toDouble)

  lazy private val jedisPool = {

    info(s"jedis pool config: redisHost: $redisHost, redisPort: $redisPort, redisAuth: $redisAuth, redisDB: $redisDB")

    new JedisPool(
      {
        val pc = new JedisPoolConfig()
        pc.setMaxTotal(redisMaxConnPerPool)
        pc.setMaxIdle(redisMaxIdlePerPool)
        pc
      },
      redisHost,
      redisPort,
      redisTimeoutInMillis,
      redisAuth.orNull,
      redisDB
    )
  }

  def getResource: Jedis = {
    jedisPool.getResource
  }

}
