package org.leletan.maiev.lib

import com.timgroup.statsd.NonBlockingStatsDClient
import org.leletan.maiev.config.{SafeConfig, StatsConifg}

/**
 * Created by jiale.tan on 1/31/18.
 */
object StatsClient
  extends StatsConifg
  with SafeConfig
{

  @transient
  lazy private val statsd = new NonBlockingStatsDClient(statsPrefix, statsHost, 8125)

  def reportCounter(metric: String, count: Long): Unit = {
    statsd.count(metric, count)
  }

  def Incr(metric: String): Unit = {
    statsd.count(metric, 1)
  }

  def reportExecutionTime(metrics: String, timeInMillis: Long): Unit = {
    statsd.recordExecutionTime(metrics, timeInMillis)
  }
}
