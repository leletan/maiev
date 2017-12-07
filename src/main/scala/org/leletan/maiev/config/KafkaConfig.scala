package org.leletan.maiev.config

/**
 * Created by jiale.tan on 12/6/17.
 */
trait KafkaConfig {
  this: SafeConfig =>

  lazy val brokers: String = safeGetConfig("spark.streaming.kafka.broker.list")
  lazy val offsetReset: String = safeGetConfig("spark.streaming.kafka.offset.reset")
  lazy val fetchMessageMaxBytes: String = getOption("spark.streaming.kafka.fetchMessageMaxBytes")
    .filter(_.nonEmpty).getOrElse("2000000")
  lazy val ignoreSavedOffsets: Boolean = safeGetConfigBoolean("spark.streaming.kafka.ignoreSavedOffsets")
  lazy val groupId: String = safeGetConfig("spark.streaming.kafka.group.id")
  lazy val zookeeperConnect: String = safeGetConfig("spark.streaming.zookeeper.connect")
  lazy val topics: Array[String] = safeGetConfig("spark.streaming.kafka.topics").split(",")
}
