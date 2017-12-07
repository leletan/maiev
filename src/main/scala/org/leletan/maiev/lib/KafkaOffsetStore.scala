package org.leletan.maiev.lib

import com.twitter.util.Future
import com.typesafe.config.ConfigFactory
import org.leletan.maiev.config.{KafkaConfig, SafeConfig}

/**
 * Created by jiale.tan on 12/6/17.
 */
trait KafkaOffsetStore {

  def get(k: ClientIdAndTopic): Future[Option[Map[Int, Long]]]

  def put(kv: (ClientIdAndTopic, Option[Map[Int, Long]])): Future[Unit]

}

object KafkaOffsetStoreFactory
  extends KafkaConfig with SafeConfig {

  private val kafkaOffsetStore = new ZookeeperKafkaOffsetStore(zookeeperConnect)

  def getKafkaOffsetStore: KafkaOffsetStore = {
    kafkaOffsetStore
  }
}