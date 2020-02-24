package com.awebone.akka_rpc

import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

class Worker extends Actor{

  override def preStart(): Unit = {
    //指定访问哪个节点上的哪个actorSystem的哪个actor
    val connectStr = "akka.tcp://MasterActorSystem@localhost:6789/user/master"
    val selection: ActorSelection = context.actorSelection(connectStr)

    selection ! "hello"
  }

  override def receive: Receive = {
    case "hi" => {
      println("master send hi")
    }

    case _ => println("非法消息")
  }
}

object WorkerRun{
  def main(args: Array[String]): Unit = {
    val hostname = "localhost"
    val strConfig =
      s"""
        |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
        |akka.remote.netty.tcp.hostname = ${hostname}
      """.stripMargin

    val config = ConfigFactory.parseString(strConfig)
    val as = ActorSystem("WorkerActorSystem", config)

    as.actorOf(Props(new Worker()), "worker")
  }
}