package com.awebone.dmp.etl

import com.awebone.dmp.Logs
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.{Dataset, SaveMode, SparkSession}

/**
  * 日志数据清洗过程
  *
  * 1）要求一：将数据转换成parquet文件格式
  * 2）要求二：序列化方式采用KryoSerializer方式
  * 3）要求三：parquet文件采用Sanppy压缩方式
  *
  *     通过处理分析，使用SparkCore只能完成KryoSerializer和Snappy，想要完成parquet比较困难，
  * 而SparkSQL处理parquet文件非常简单，所以需要将原先的编码做一稍微改动
  */
object DMPLogETLHDFSOps {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.hadoop").setLevel(Level.WARN)
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.spark-project").setLevel(Level.WARN)

    if(args == null || args.length < 2){
      println(
        """Parameter Errors! Usage: <inputpath> <outputpath>
          |inputpath  : input path
          |outputpath : output path
        """.stripMargin)
      System.exit(-1)
    }
    val Array(inputpath, outputpath) = args

    val conf: SparkConf = new SparkConf().setAppName("DMPLogETL").setMaster("local[*]")
      .set("spark.serializer",classOf[KryoSerializer].getName)
      .registerKryoClasses(Array(classOf[Logs])) //要求二：序列化方式采用KryoSerializer方式
    val spark: SparkSession = SparkSession.builder().config(conf).getOrCreate()
    import spark.implicits._

    val lines:RDD[String] = spark.sparkContext.textFile(inputpath)

    val retDS: Dataset[Logs] = lines.map(line => {
      val log: Logs = Logs.line2Logs(line)
      log
    }).toDS()

    /**
      * 要求一：将数据转换成parquet文件格式
      * 要求三：parquet文件采用Sanppy压缩方式
      */
    retDS.write.mode(SaveMode.Overwrite).parquet(outputpath)

    spark.stop()
  }
}
