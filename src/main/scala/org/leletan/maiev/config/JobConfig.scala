package org.leletan.maiev.config

/**
 * Created by jiale.tan on 12/28/17.
 */
trait JobConfig {
  this: SafeConfig =>
  lazy val jobLogLevel: String = safeGetConfig("spark.job.log.level")
}
