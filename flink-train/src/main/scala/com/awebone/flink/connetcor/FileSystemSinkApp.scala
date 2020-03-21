package com.awebone.flink.connetcor


import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.fs.StringWriter
import org.apache.flink.streaming.connectors.fs.bucketing.{BucketingSink, DateTimeBucketer}

object FileSystemSinkApp {
  def main(args: Array[String]): Unit = {
    System.setProperty("HADOOP_USER_NAME","hadoop")
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    val data = env.socketTextStream("hadoop04",9999)

    data.print().setParallelism(1)
    val filepath = "/tmpdata/flink/hdfssink"

    val sink = new BucketingSink[String](filepath)
    sink.setBucketer(new DateTimeBucketer[String]("yyyy-MM-dd--HHmm"))
    sink.setWriter(new StringWriter())
    sink.setBatchRolloverInterval(20)

    data.addSink(sink)
    env.execute("FileSystemSinkApp")
  }
}
