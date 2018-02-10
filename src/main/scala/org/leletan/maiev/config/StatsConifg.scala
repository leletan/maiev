package org.leletan.maiev.config

/**
 * Created by jiale.tan on 1/31/18.
 */
trait StatsConifg {
  this: SafeConfig =>
  lazy val statsHost: String = safeGetConfig("stats.host")
  lazy val statsPrefix: String = safeGetConfig("stats.prefix")
}
