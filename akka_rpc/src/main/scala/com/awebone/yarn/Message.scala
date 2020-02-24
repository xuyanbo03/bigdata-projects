package com.awebone.yarn

//样例类，做模式匹配

//注册消息   nodemanager  -> resourcemanager
case class RegisterNodeManager(val nodemanagerid: String, val memory: Int, val cpu: Int)

//资源： 不是说哪个任务需要多少资源，就把资源给这个任务
//而是，某个节点有多少适合用于做计算的资源，那么就把这个任务启动在这个节点上


//注册完成消息 resourcemanager -》 nodemanager
case class RegisteredNodeManager(val resourcemanagerhostname: String)


//心跳消息  nodemanager -》 resourcemanager
case class Heartbeat(val nodemanagerid: String)

/**
  * 是在RM中，为了维持整个集群中，到底哪个节点有多少资源
  * 所以吧每个节点的资源都封装在一个NodeManagerInfo对象里
  * 然后在RM中就维持了一个NodeManagerInfo对象的集合
  */
class NodeManagerInfo(val nodemanagerid: String, val memory: Int, val cpu: Int) {
  //用来存储nomanagerid这个NodeManager的最后一次心跳时间
  //_是一个默认值
  var lastHeartBeatTime: Long = _
}

//单例
case object SendMessage //仅仅是一个标志
case object CheckTimeOut //也是一个标志