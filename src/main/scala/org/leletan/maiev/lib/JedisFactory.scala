package org.leletan.maiev.lib

import org.leletan.maiev.config.{RedisClientConfig, SafeConfig}
import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

/**
 * Created by jiale.tan on 1/31/18.
 */
@transient
object JedisFactory
  extends RedisClientConfig
    with SafeConfig {

  lazy private val jedisPool = new JedisPool(
    {
      val pc = new JedisPoolConfig()
      pc.setMaxTotal(128)
      pc
    },
    redisHost,
    redisPort,
    5000,
    redisAuth,
    redisDB
  )

  def getConn: Jedis = {
    jedisPool.getResource
  }

}
