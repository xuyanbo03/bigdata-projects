package com.awebone.dmp.report

import java.util.Properties

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.sql.{DataFrame, SaveMode, SparkSession}

/**
  * 省份：province
  * 城市：city
  * 结果存储到MySQL数据库
  * select
  *     province,
  *     city,
  *     count(1)
  * from logs
  * group by province, city
  **/
object ProvinceCityQuantityJob {
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

    val conf: SparkConf = new SparkConf().setAppName("ProvinceCityQuantityJob").setMaster("local[*]")
    val spark: SparkSession = SparkSession.builder().config(conf).getOrCreate()

    val input: DataFrame = spark.read.parquet(inputpath)
    input.createOrReplaceTempView("logs")

    val sql =
      """
        |select
        |    date_sub(current_date(), 0) data_date,
        |    provincename province,
        |    cityname city,
        |    count(1) as countz
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
