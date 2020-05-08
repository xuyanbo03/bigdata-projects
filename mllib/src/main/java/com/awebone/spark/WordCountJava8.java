package com.awebone.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.Arrays;

public class WordCountJava8 {
	public static void main(String[] args) {
		//获取程序入口
		SparkConf sparkConf = new SparkConf();
		sparkConf.setAppName("WordCountJava8");
		sparkConf.setMaster("local");
		JavaSparkContext javaSparkContext = new JavaSparkContext(sparkConf);

		//获取数据
		JavaRDD<String> linesRDD = javaSparkContext.textFile("hdfs://myha/wc/input");

		//计算
		JavaRDD<String> rdd1 = linesRDD.flatMap(s -> Arrays.asList(s.split(" ")).iterator());
		JavaPairRDD<String, Integer> rdd2 = rdd1.mapToPair(s -> new Tuple2<>(s, 1));
		JavaPairRDD<String, Integer> rdd3 = rdd2.reduceByKey((x, y) -> x + y);

		rdd3.foreach(t -> System.out.println(t._1 + "\t" + t._2));

		javaSparkContext.stop();
	}
}
