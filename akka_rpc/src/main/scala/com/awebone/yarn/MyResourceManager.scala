package com.awebone.yarn

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable

class MyResourceManager(var hostname: String, var port: Int) extends Actor {

  // 用来存储每个注册的NodeManager节点的信息
  private var id2nodemanagerinfo = new mutable.HashMap[String, NodeManagerInfo]()
  // 对所有注册的NodeManager进行去重，其实就是一个HashSet
  private var nodemanagerInfoes = new mutable.HashSet[NodeManagerInfo]()

  // actor在最开始的时候，会执行一次
  override def preStart(): Unit = {
    import scala.concurrent.duration._
    import context.dispatcher

    // 调度一个任务， 每隔五秒钟执行一次，每隔5秒给自己发送一次信息
    context.system.scheduler.schedule(0 millis, 5000 millis, self, CheckTimeOut)
  }

  override def receive: Receive = {

    case RegisterNodeManager(nodemanagerid, memory, cpu) => {
      val nodeManagerInfo = new NodeManagerInfo(nodemanagerid, memory, cpu)

      // 对注册的NodeManager节点进行存储管理
      id2nodemanagerinfo.put(nodemanagerid, nodeManagerInfo)
      nodemanagerInfoes += nodeManagerInfo

      //把信息存到zookeeper
      sender() ! RegisteredNodeManager(hostname + ":" + port)
    }

    case Heartbeat(nodemanagerid) => {
      val currentTime = System.currentTimeMillis()
      val nodeManagerInfo = id2nodemanagerinfo(nodemanagerid)
      nodeManagerInfo.lastHeartBeatTime = currentTime

      id2nodemanagerinfo(nodemanagerid) = nodeManagerInfo
      nodemanagerInfoes += nodeManagerInfo
    }

    // 检查过期失效的 NodeManager
    case CheckTimeOut => {
      val currentTime = System.currentTimeMillis()

      // 15 秒钟失效
      //foreach：遍历
      //filter：拿到所有的已经宕机的节点
      nodemanagerInfoes.filter(nm => currentTime - nm.lastHeartBeatTime > 15000)
        .foreach(deadnm => {
          nodemanagerInfoes -= deadnm
          id2nodemanagerinfo.remove(deadnm.nodemanagerid)
        })
      println("当前注册成功的节点数" + nodemanagerInfoes.size);
    }
  }
}

object MyResourceManager {
  def main(args: Array[String]): Unit = {
    val RESOURCEMANAGER_HOSTNAME = args(0) //解析的配置的日志
    val RESOURCEMANAGER_PORT = args(1).toInt

    //解析运行时所需要的参数
    val str =
      s"""
         |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
         |akka.remote.netty.tcp.hostname =${RESOURCEMANAGER_HOSTNAME}
         |akka.remote.netty.tcp.port=${RESOURCEMANAGER_PORT}
      """.stripMargin

    val conf = ConfigFactory.parseString(str)
    val actorSystem = ActorSystem(Constant.RMAS, conf)

    //启动一个actor
    actorSystem.actorOf(Props(new MyResourceManager(RESOURCEMANAGER_HOSTNAME, RESOURCEMANAGER_PORT)), Constant.RMA)
  }
}
