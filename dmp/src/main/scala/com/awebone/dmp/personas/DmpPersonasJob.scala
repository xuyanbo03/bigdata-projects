package com.awebone.dmp.personas

import java.io.FileInputStream
import java.util.Properties

import com.awebone.dmp.Logs
import com.awebone.dmp.constants.AdTagConstants
import com.awebone.dmp.tags._
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client.{ConnectionFactory, Put}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.collection.{JavaConversions, mutable}

/**
  * dmp用户画像便签统计
  */
object DmpPersonasJob {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.hadoop").setLevel(Level.WARN)
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.spark-project").setLevel(Level.WARN)

    if (args == null || args.length < 1) {
      println(
        """Parameter Errors! Usage: <inputpath>
          |inputpath  : input path
        """.stripMargin)
      System.exit(-1)
    }
    val Array(inputpath) = args

    val conf: SparkConf = new SparkConf().setAppName("DmpPersonasJob").setMaster("local[*]")
    val spark: SparkSession = SparkSession.builder().config(conf).getOrCreate()
    import spark.implicits._

    val input: Dataset[Logs] = spark.read.parquet(inputpath).as[Logs]
    val logs: RDD[Logs] = input.rdd

    //提取用户的标签 |<userid, map> |
    val userid2Tags: RDD[(String, Map[String, Int])] = logs.map { case logs: Logs => {
      var userid: String = logs.userid
      if (userid == null) {
        userid = getNotEmptyID(logs).getOrElse("UnKnown")
      }

      val adspaceTags: Map[String, Int] = AdPositionTag.extractTag(logs)
      val appTags: Map[String, Int] = AppTag.extractTag(logs)
      val channelTags: Map[String, Int] = ChannelTag.extractTag(logs)
      val deviceTags: Map[String, Int] = DeviceTag.extractTag(logs)
      val kwTags: Map[String, Int] = KeyWordTag.extractTag(logs)
      val areaTags: Map[String, Int] = AreaTag.extractTag(logs)

      (userid, adspaceTags.++(appTags).++(channelTags).++(deviceTags).++(kwTags).++(areaTags))
    }
    }

    //map1<kw-zs,1>  map2<kw-zs,2> --><kw-zs,3>
    val userid2AggrTags: RDD[(String, Map[String, Int])] = userid2Tags.reduceByKey { case (map1, map2) => {
      val map = mutable.Map[String, Int]()
      map.++=(map1)

      for ((k, v) <- map2) {
        map.put(k, map.getOrElse(k, 0) + v)
      }
      map.toMap
    }
    }
    //    userid2AggrTags.foreach(println)
    //    (2,Map(NET_3 -> 2, ZC_益阳市 -> 2, DEVICE_1 -> 2, APP_其他 -> 2, ZP_湘南省 -> 2, LC_02 -> 2, ISP_4 -> 2, CN_ -> 2))
    //    (1,Map(ZP_上海市 -> 2, NET_3 -> 2, DEVICE_1 -> 2, APP_马上赚 -> 2, LC_02 -> 2, ISP_4 -> 2, CN_ -> 2, ZC_上海市 -> 2))

    //转换属性
    val props = loadProerties()
    val propsBC = spark.sparkContext.broadcast(props)

    val aggrTags = userid2AggrTags.map{case (userid, tagMap) => {
      val map = mutable.Map[String, Int]()
      val propsMap = propsBC.value

      for((k,v) <- tagMap){
        var key = k

        if(k.contains(AdTagConstants.PREFIX_AD_DEVICE_TAG)){
          val dMap = propsMap(AdTagConstants.PREFIX_AD_DEVICE_TAG)
          val id = k.split("_")(1)
          val dName = dMap.get(id).get.split("\\s+")(1)
          //k --> prefix_id
          key = AdTagConstants.PREFIX_AD_DEVICE_TAG + dName
        }else if(k.contains(AdTagConstants.PREFIX_AD_ISP_TAG)) {
          val ispMap = propsMap(AdTagConstants.PREFIX_AD_ISP_TAG)
          val id = k.split("_")(1)
          val ispName = ispMap.get(id).get.split("\\s+")(1)
          key = AdTagConstants.PREFIX_AD_ISP_TAG + ispName
        } else if(k.contains(AdTagConstants.PREFIX_AD_NETWORK_TAG)) {
          val nwMap = propsMap(AdTagConstants.PREFIX_AD_NETWORK_TAG)
          val id = k.split("_")(1)
          val nwName = nwMap.get(id).get.split("\\s+")(1)
          key =  AdTagConstants.PREFIX_AD_NETWORK_TAG + nwName
        }
        map.put(key, v)
      }

      (userid, map)
    }}

    /**
      * 将标签聚合结果存储到hbase中
      * 因为，经过我们分析，计算得出的标签可能半结构化的数据，同时如果在dmp和dsp中进行交互的时候，流量比较大的情况下
      * 我们使用mysql没有办法保证时效性，所以我们这里使用hbase进行存储
      * create_space bigdata
      * create 'bigdata:dmp_tag', 'cf'
      * HBase api
      */
    aggrTags.foreachPartition(partition => {
      if(partition != null){
        val connection = ConnectionFactory.createConnection(HBaseConfiguration.create())
        val table = connection.getTable(TableName.valueOf("bigdata:dmp_tag"))

        partition.foreach{case (userid, tagMap) => {
          val put = new Put(userid.getBytes())

          //tagMap--[Deivce_xxxx, 5]
          for((col,value) <- tagMap){
            put.addColumn("cf".getBytes(), col.getBytes(), value.toString.getBytes())
          }
          table.put(put)
        }}

        table.close()
        connection.close()
      }
    })

    spark.stop()
  }

  /**
    * 加载配置文件
    * type
    *   device  k, value
    *   isp
    *   network
    */
  def loadProerties():mutable.Map[String, mutable.Map[String, String]] = {
    val props = mutable.Map[String, mutable.Map[String, String]]()
    val properties = new Properties()

    //加载deivce
    properties.load(new FileInputStream("data/device-mapping.dic"))
    val deviceMap = mutable.Map[String, String]()

    for (dk <- JavaConversions.asScalaSet(properties.keySet())){
      deviceMap.put(dk.toString,properties.getProperty(dk.toString))
    }
    props.put(AdTagConstants.PREFIX_AD_DEVICE_TAG, deviceMap)

    //加载isp
    properties.clear()
    properties.load(new FileInputStream("data/isp-mapping.dic"))
    val ispMap = mutable.Map[String, String]()
    for(dk <- JavaConversions.asScalaSet(properties.keySet())) {
      ispMap.put(dk.toString, properties.getProperty(dk.toString))
    }
    props.put(AdTagConstants.PREFIX_AD_ISP_TAG, ispMap)

    //network
    properties.clear()
    properties.load(new FileInputStream("data/network-mapping.dic"))
    val nwMap = mutable.Map[String, String]()
    for(dk <- JavaConversions.asScalaSet(properties.keySet())) {
      nwMap.put(dk.toString, properties.getProperty(dk.toString))
    }
    props.put(AdTagConstants.PREFIX_AD_NETWORK_TAG, nwMap)

    props
  }


  // 获取用户唯一不为空的ID
  def getNotEmptyID(log: Logs): Option[String] = {
    log match {
      case v if v.imei.nonEmpty => Some("IMEI:" + v.imei.replaceAll(":|-\\", "").toUpperCase)
      case v if v.imeimd5.nonEmpty => Some("IMEIMD5:" + v.imeimd5.toUpperCase)
      case v if v.imeisha1.nonEmpty => Some("IMEISHA1:" + v.imeisha1.toUpperCase)

      case v if v.androidid.nonEmpty => Some("ANDROIDID:" + v.androidid.toUpperCase)
      case v if v.androididmd5.nonEmpty => Some("ANDROIDIDMD5:" + v.androididmd5.toUpperCase)
      case v if v.androididsha1.nonEmpty => Some("ANDROIDIDSHA1:" + v.androididsha1.toUpperCase)

      case v if v.mac.nonEmpty => Some("MAC:" + v.mac.replaceAll(":|-", "").toUpperCase)
      case v if v.macmd5.nonEmpty => Some("MACMD5:" + v.macmd5.toUpperCase)
      case v if v.macsha1.nonEmpty => Some("MACSHA1:" + v.macsha1.toUpperCase)

      case v if v.idfa.nonEmpty => Some("IDFA:" + v.idfa.replaceAll(":|-", "").toUpperCase)
      case v if v.idfamd5.nonEmpty => Some("IDFAMD5:" + v.idfamd5.toUpperCase)
      case v if v.idfasha1.nonEmpty => Some("IDFASHA1:" + v.idfasha1.toUpperCase)

      case v if v.openudid.nonEmpty => Some("OPENUDID:" + v.openudid.toUpperCase)
      case v if v.openudidmd5.nonEmpty => Some("OPENDUIDMD5:" + v.openudidmd5.toUpperCase)
      case v if v.openudidsha1.nonEmpty => Some("OPENUDIDSHA1:" + v.openudidsha1.toUpperCase)

      case _ => None
    }
  }
}
