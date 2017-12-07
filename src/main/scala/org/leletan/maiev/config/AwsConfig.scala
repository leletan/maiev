package org.leletan.maiev.config


trait AwsConfig {
  this: SafeConfig =>

  lazy val s3Bucket = safeGetConfig("spark.s3.bucket")
  lazy val s3Prefix = safeGetConfig("spark.s3.prefix")

}
