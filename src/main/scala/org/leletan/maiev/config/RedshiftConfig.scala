package org.leletan.maiev.config

/**
 * Created by jiale.tan on 1/30/18.
 */
trait RedshiftConfig {
  this: SafeConfig =>
  lazy val redshiftDBName: String = safeGetConfig("spark.redshift.db.name")
  lazy val redshiftUserId: String = safeGetConfig("spark.redshift.user.id")
  lazy val redshiftPassword: String = safeGetConfig("spark.redshift.password")
  lazy val redshifturl: String = safeGetConfig("spark.redshift.url")
  lazy val tempS3Dir: String = safeGetConfig("spark.redshift.temp.s3.dir")

  lazy val redshiftJDBCURL: String =
    s"jdbc:redshift://$redshifturl/$redshiftDBName?user=$redshiftUserId&password=$redshiftPassword"

}
