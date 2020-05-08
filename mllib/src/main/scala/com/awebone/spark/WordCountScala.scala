package com.awebone.spark

import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object WordCountScala {
  def main(args: Array[String]): Unit = {
    //获取程序入口
    val sparkConf: SparkConf = new SparkConf()
    sparkConf.setAppName(WordCountScala.getClass.getSimpleName)
    sparkConf.setMaster("local")
    val sparkContext: SparkContext = new SparkContext(sparkConf)

    //WorkCount
    val linesRDD: RDD[String] = sparkContext.textFile(args(0))
    val wordRDD: RDD[String] = linesRDD.flatMap(_.split(" "))
    val wordAndOneRDD: RDD[(String, Int)] = wordRDD.map((_, 1))
    val wordsCountRDD = wordAndOneRDD.reduceByKey((x: Int, y: Int) => x + y)
    wordsCountRDD.foreach(x => println(x._1, x._2))
    wordsCountRDD.saveAsTextFile(args(1))

    sparkContext.stop()
  }

}
