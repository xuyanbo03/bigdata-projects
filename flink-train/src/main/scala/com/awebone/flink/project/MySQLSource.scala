package com.awebone.flink.project


import java.sql.{Connection, DriverManager, PreparedStatement}

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, SourceFunction}

import scala.collection.mutable

/**
  * 自定义Mysql 并行的Source
  */
class MySQLSource extends RichParallelSourceFunction[mutable.HashMap[String, String]] {
  var connection: Connection = null
  var ps: PreparedStatement = null

  //创建连接
  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://hadoop01:3306/flink"
    val user = "root"
    val password = "root"
    Class.forName(driver)
    connection = DriverManager.getConnection(url, user, password)

    val sql = "select user_id,domain from user_domain_config"
    ps = connection.prepareStatement(sql)
  }

  //不断执行的函数
  override def run(sourceContext: SourceFunction.SourceContext[mutable.HashMap[String, String]]): Unit = {
    val resultSet = ps.executeQuery()
    val collect = mutable.HashMap[String,String]()

    //将查询结果放入HashMap中
    while (resultSet.next()){
      collect.put(resultSet.getNString("domain"), resultSet.getNString("user_id"))
    }
    sourceContext.collect(collect)
  }

  override def cancel(): Unit = {}

  override def close(): Unit = {
    if(ps != null){
      ps.close()
    }
    if(connection != null){
      connection.close()
    }
  }
}
