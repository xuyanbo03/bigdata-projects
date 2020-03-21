package com.awebone.flink.project

import java.text.SimpleDateFormat
import java.util.{Date, Properties}

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.elasticsearch.{ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.elasticsearch6.ElasticsearchSink
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector
import org.apache.http.HttpHost
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.Requests
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object LogAnalysisWithMySQL {
  def main(args: Array[String]): Unit = {
    //在生产上进行日志的输出，采用以下方式
    val logger = LoggerFactory.getLogger("LogAnalysis")

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    //设置事件时间作为flink处理的基准时间
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    import org.apache.flink.api.scala._

    /**
      * 读取kafka集群数据
      */
    val topic = "cdnlog"
    val properties: Properties = new Properties()
    properties.setProperty("bootstrap.servers","hadoop01:9092,hadoop02:9092,hadoop03:9092,hadoop04:9092")
    properties.setProperty("zookeeper.connect", "hadoop02:2181,hadoop03:2181,hadoop01:2181/kafka") //声明zk
    properties.setProperty("group.id","test-cdnlog-mysql")

    val consumer = new FlinkKafkaConsumer[String](topic, new SimpleStringSchema(), properties)
    val data = env.addSource(consumer) // 接受kafka数据
    //    data.print().setParallelism(1) // 测试是否连通

    /**
      * 数据清洗：
      * 在生产上进行业务处理的时候，一定要考虑处理的健壮性以及数据的准确性
      * 脏数据或者是不符合业务规则的数据是需要全部过滤掉之后
      * 再进行相应业务逻辑的处理
      */
    val logData = data.map(x => {
      val strings = x.split("\t")

      val level = strings(2)
      val timeStr = strings(3)
      var time = 0l
      try {
        val sourceFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        time = sourceFormat.parse(timeStr).getTime
      } catch {
        case e:Exception => {
          logger.error(s"time parse error: $timeStr", e.getMessage)
        }
      }

      val domain = strings(5)
      val traffic = strings(6).toLong
      (level, time, domain, traffic)
    }).filter(_._2 != 0).filter(_._1 == "E")
      .map(x => {
        (x._2, x._3, x._4) //数据清洗按照业务规则取相关数据 1level(不需要可以抛弃) 2time 3 domain 4traffic
      })

    /**
      * 连接mysql，合并字段
      */
    val mysqlData = env.addSource(new MySQLSource)
//    mysqlData.print()
    val connectData = logData.connect(mysqlData)
        .flatMap(new CoFlatMapFunction[(Long, String, Long), mutable.HashMap[String, String], (Long, String, Long, String)] {
          var userDomainMap: mutable.HashMap[String, String] = mutable.HashMap[String, String]()

          //log
          override def flatMap1(in1: (Long, String, Long), collector: Collector[(Long, String, Long, String)]): Unit = {
            val domain = in1._2
            val userId = userDomainMap.getOrElse(domain, "")
//            collector.collect(in1._1 + "\t" + in1._2 + "\t" + in1._3 + "\t" + userId)
            collector.collect((in1._1, domain, in1._3, userId))
          }

          override def flatMap2(in2: mutable.HashMap[String, String], collector: Collector[(Long, String, Long, String)]): Unit = {
            userDomainMap = in2
          }
        })

//    connectData.print()

    /**
      *   设置timestamp和watermark,解决时序性问题
      *   AssignerWithPeriodicWatermarks[T] 对应logdata的tuple类型
      */
    val resultData = connectData.assignTimestampsAndWatermarks(new AssignerWithPeriodicWatermarks[(Long, String, Long, String)] {
      //最大无序容忍的时间 10s
      val maxOutOfOrderness = 10000L // 3.5 seconds
      //当前最大的TimeStamp
      var currentMaxTimestamp: Long = _

      //设置TimeStamp生成WaterMark
      override def getCurrentWatermark: Watermark = {
        new Watermark(currentMaxTimestamp - maxOutOfOrderness)
      }

      //抽取时间
      override def extractTimestamp(element: (Long, String, Long, String), previousElementTimestamp: Long): Long = {
        //获取数据的event time
        val timestamp: Long = element._1
        currentMaxTimestamp = Math.max(timestamp, currentMaxTimestamp)
        timestamp
      }
    }) //根据window进行业务逻辑的处理   最近一分钟每个用户产生的流量
      .keyBy(3) //以userid进行分组
      .window(TumblingEventTimeWindows.of(Time.seconds(60))) //每60秒为一个窗口，进行统计
      .apply(new WindowFunction[(Long, String, Long, String), (String, String, Long, String), Tuple, TimeWindow] {
        override def apply(key: Tuple, window: TimeWindow, input: Iterable[(Long, String, Long, String)], out: Collector[(String, String, Long, String)]): Unit = {
          val userid = key.getField(0).toString //拿到key，userid

          var sum = 0l
          val times = ArrayBuffer[Long]()
          val iterator = input.iterator
          while (iterator.hasNext) {
            val next = iterator.next()
            sum += next._3 //统计流量
            times.append(next._1) //记录这一分钟，格式：yyyy-MM-dd HH:mm
          }
          val time = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(times.max)) // 这一分钟的时间，格式化

          /**
            * 输出结果：
            * 第一个参数：这一分钟的时间
            * 第二个参数：域名
            * 第三个参数：traffic流量的和
            */
          out.collect((time, domain, sum, userid))
        }
      })
    resultData.print().setParallelism(1)


    /**
      * 连接es库，导入数据
      * 使用kibana可视化
      */
    val httpHosts = new java.util.ArrayList[HttpHost]
    httpHosts.add(new HttpHost("redhat", 9200, "http"))

    val esSinkBuilder = new ElasticsearchSink.Builder[(String, String, Long, String)](
      httpHosts,
      new ElasticsearchSinkFunction[(String, String, Long, String)] {
        override def process(t: (String, String, Long, String), runtimeContext: RuntimeContext, requestIndexer: RequestIndexer): Unit = {
          requestIndexer.add(createIndexRequest(t))
        }

        def createIndexRequest(element: (String, String, Long, String)): IndexRequest = {
          val json = new java.util.HashMap[String, Any]
          json.put("time", element._1)
          json.put("domain", element._2)
          json.put("traffics", element._3)
          json.put("userid", element._4)
          val id = element._1 + "-" + element._2
          return Requests.indexRequest()
            .index("cdn")
            .`type`("traffic-userid")
            .id(id)
            .source(json)
        }
      }
    )

    //设置要为每个批量请求缓冲的最大操作数
    esSinkBuilder.setBulkFlushMaxActions(1)
    resultData.addSink(esSinkBuilder.build()) //.setParallelism(5)

    env.execute("LogAnalysisWithMySQL")
  }
}
