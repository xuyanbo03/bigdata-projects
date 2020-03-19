package com.awebone.dmp.report

import java.util.Properties

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

/**
  * 广告请求地域分布统计
  * 省市/城市	   总请求	有效请求	广告请求	|参与竞价数	竞价成功数 	竞价成功率	|展示量	点击量	点击率	|广告成本	广告消费
  *  汇总结果，是可以保存到mysql(hbase)表中的，全量结果不建议保存到mysql
  */
object AreaRequestDistributionJob {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.hadoop").setLevel(Level.WARN)
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.spark-project").setLevel(Level.WARN)

    if(args == null || args.length < 2){
      println(
        """Parameter Errors! Usage: <inputpath> <table>
          |inputpath  : input path
          |table :  mysql table name
        """.stripMargin)
      System.exit(-1)
    }
    val Array(inputpath, table) = args

    val conf: SparkConf = new SparkConf().setAppName("AreaRequestDistributionJob").setMaster("local[*]")
    val spark: SparkSession = SparkSession.builder().config(conf).getOrCreate()

    val input: DataFrame = spark.read.parquet(inputpath)
    input.createOrReplaceTempView("logs")

    val sql =
      """
        |select
        |    date_sub(current_date(), 1) data_date,
        |    provincename province,
        |    cityname city,
        |    sum(if(requestmode = 1 and processnode >= 1, 1, 0)) orginal_req,
        |    sum(if(requestmode = 1 and processnode >= 2, 1, 0)) valid_req,
        |    sum(if(requestmode = 1 and processnode = 3, 1, 0)) ad_req,
        |    sum(case when ADPlatformProviderID >=100000 and iseffective = 1 and isbilling = 1 and isbid = 1 and adorderid != 0
        |         then 1
        |         else 0
        |    end) tpi_bid_num,
        |    sum(case when ADPlatformProviderID >=100000 and iseffective = 1 and isbilling = 1 and iswin = 1
        |         then 1
        |         else 0
        |    end) win_bid_num,
        |    sum(case when requestmode = 2 and iseffective = 1
        |         then 1
        |         else 0
        |    end) show_ad_master_num,
        |    sum(case when requestmode = 3 and iseffective = 1
        |         then 1
        |         else 0
        |    end) click_ad_master_num,
        |    sum(case when requestmode = 2 and iseffective = 1 and isbilling = 1
        |         then 1
        |         else 0
        |    end) show_ad_media_num,
        |    sum(case when requestmode = 3 and iseffective = 1 and isbilling = 1
        |         then 1
        |         else 0
        |    end) click_ad_media_num,
        |    round(sum(case when ADPlatformProviderID >=100000 and iseffective = 1 and isbilling = 1 and iswin = 1 and adorderid >=200000 and adcreativeid >=200000
        |         then winprice
        |         else 0.0
        |    end) / 1000, 2) dsp_ad_xf,
        |    round(sum(case when ADPlatformProviderID >=100000 and iseffective = 1 and isbilling = 1 and iswin = 1 and adorderid >=200000 and adcreativeid >=200000
        |         then adpayment
        |         else 0.0
        |    end) / 1000, 2) dsp_ad_cost
        |from logs
        |group by provincename, cityname
      """.stripMargin

    val url = "jdbc:mysql://hadoop01:3306/dmp"
    val properties = new Properties
    properties.put("user","root")
    properties.put("password","root")

    spark.sql(sql).write.mode(SaveMode.Append).jdbc(url,table,properties)

    spark.stop()
  }
}
