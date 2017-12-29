package org.leletan.maiev.config

/**
 * Created by jiale.tan on 12/23/17.
 */
trait JDBCConfig {
  this: SafeConfig =>
  lazy val jdbcDriver: String = safeGetConfig("spark.jdbc.driver")
  lazy val jdbcURL: String = safeGetConfig("spark.jdbc.url")
  lazy val jdbcUser: String = safeGetConfig("spark.jdbc.user")
  lazy val jdbcPassword: String = safeGetConfig("spark.jdbc.password")
}
