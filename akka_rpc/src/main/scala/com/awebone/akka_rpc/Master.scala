package com.awebone.akka_rpc

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

class Master extends Actor{

  override def preStart(): Unit = {
    //业务逻辑初始化
    println("prestart")
  }

  //相当于是一个run，处理业务逻辑时有消息传送过来
  override def receive: Receive = {
    case "hello" => {
      //这个注释代表模拟一个业务方法，得到结果
      println("receive hi")

      val result = "hi"
      //谁发送过来消息，谁就是sender()
      sender() ! result
    }

    case _ => println("非法新消息")
  }
}

object MasterRun{
  def main(args: Array[String]): Unit = {
    val strConfig =
      """
        |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
        |akka.remote.netty.tcp.hostname =localhost
        |akka.remote.netty.tcp.port=6789
      """.stripMargin

    val config = ConfigFactory.parseString(strConfig)
    val as = ActorSystem("MasterActorSystem",config)

    as.actorOf(Props(new Master()), "master")
    println("MasterActorSystem init")
  }
}