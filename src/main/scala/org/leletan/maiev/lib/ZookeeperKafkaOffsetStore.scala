package org.leletan.maiev.lib

import scala.collection.JavaConverters._
import com.twitter.{util => twitter}
import com.twitter.util._
import com.twitter.zk.{ZNode, ZkClient}
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException.{NoNodeException, NodeExistsException, NotEmptyException}
import org.apache.zookeeper.ZooDefs.Ids


/**
 * Created by jiale.tan on 12/4/17.
 */

case class KafkaMetadata(topic: String, partition: Int, from: Long, to: Long)

case class ClientIdAndTopic(clientId: String, topic: String)

class ZookeeperKafkaOffsetStore(zkClient: ZkClient) extends KafkaOffsetStore with Logger {

  def this(host: String) = this(
    ZkClient(host, None, twitter.Duration.fromMilliseconds(3000))(new JavaTimer())
      .withAcl(Ids.OPEN_ACL_UNSAFE.asScala)
      .withMode(CreateMode.PERSISTENT)
      .withRetries(3)
  )

  def get(k: ClientIdAndTopic): Future[Option[Map[Int, Long]]] = {
    (for {
      nodeWithChildren <- getNodeWithChildren(k)
      allPartitionsOffsetData <- getAllChildrenNodesData(nodeWithChildren)
    } yield {
      if (allPartitionsOffsetData.isEmpty) {
        warn(s"Kafka consumer node exists for $k " +
          s"but failed to fetch partition info")
        None
      } else {
        info(s"Kafka consumer node partition info for $k:\n$allPartitionsOffsetData")
        Some(allPartitionsOffsetData)
      }
    }).handle {
      case NonFatal(e) =>
        error(e, Some(s"Failed to fetch partition info for Kafka consumer node $k"))
        None
    }
  }

  def put(kv: (ClientIdAndTopic, Option[Map[Int, Long]])): Future[Unit] = {
    val parentPath = s"/consumers/${kv._1.clientId}/offsets/${kv._1.topic}"
    kv match {
      case (k, Some(v)) =>
        Future.collect(
          v.map {
            case (partition, offset) =>
              for {
                node <- createPath(s"$parentPath/${partition.toString}")
                setData <- node.setData(offset.toString.getBytes, -1)
                logging <- Future(info(s"Saved kafka offset for $k: $partition -> $offset"))
              } yield {
                logging
              }
          }.toSeq
        )
          .map {
            _ =>
              info(s"Saved all ${v.size} kafka offsets for $k")
          }
      case (k, None) =>
        forceDeleteNode(parentPath)
          .map {
            _ =>
              info(s"Deleted all kafka offsets for $k")
          }
    }
  }

  private def getNodeWithChildren(k: ClientIdAndTopic): Future[ZNode.Children] = {
    zkClient
      .apply(s"/consumers/${k.clientId}/offsets/${k.topic}")
      .getChildren
      .apply()
  }

  private def getAllChildrenNodesData(nodeWithChildren: ZNode.Children): Future[Map[Int, Long]] = {
    Future.collect(
      nodeWithChildren.children.map {
        child =>
          child.getData.apply().map {
            partition =>
              (partition.path.split("/").last.toInt,
                String.valueOf(partition.bytes.map(_.toChar)).toLong)
          }
      }
    ).map(_.toMap)
  }

  private def createPath(path: String): Future[ZNode] = {
    def forceCreateNode(pathSgmnt: String): Future[ZNode] = {
      val node = zkClient.apply(pathSgmnt)
      node
        .create()
        .handle {
          case e: NodeExistsException =>
            // Ignore node exists exception
            node
        }
    }

    path.split("/+")
      .filter(_.length > 0)
      .foldLeft[Future[ZNode]](forceCreateNode("/")) {
      (parentNodeFuture, child) =>
        parentNodeFuture.flatMap {
          parentNode =>
            forceCreateNode(parentNode.childPath(child))
        }
    }
  }

  private def forceDeleteNode(path: String, version: Int = 0): Future[ZNode] = {
    val node = zkClient.apply(path)
    info(s"Deleting node at $path")
    node
      .delete(version)
      .rescue {
        case e: NoNodeException =>
          error(e, Some("Failed to delete a non-exist node"))
          // Ignore node not found exception
          Future(node)
        case e: NotEmptyException =>
          // force delete all child node
          for {
            nodeWithChildren <- node.getChildren.apply()
            deleteAllChildren <- Future.collect(
              nodeWithChildren.children.map {
                child =>
                  child
                    .exists
                    .apply()
                    .map(_.stat.getVersion)
                    .flatMap(forceDeleteNode(child.path, _))
              }
            )
          } yield
            node
      }
  }
}
