package com.awebone.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Iterator;

public class WordCountJava7 {
	public static void main(String[] args) {
		//获取程序入口
		SparkConf sparkConf = new SparkConf();
		sparkConf.setAppName("WordCountJava7");
		sparkConf.setMaster("local");
		JavaSparkContext javaSparkContext = new JavaSparkContext(sparkConf);

		//获取数据
		JavaRDD<String> linesRDD = javaSparkContext.textFile("hdfs://myha/wc/input");

		//计算
		JavaRDD<String> wordsRDD = linesRDD.flatMap(new FlatMapFunction<String, String>() {
			@Override
			public Iterator<String> call(String s) throws Exception {
				return Arrays.asList(s.split(" ")).iterator();
			}
		});

		JavaPairRDD<String, Integer> wordAndOneRDD = wordsRDD.mapToPair(new PairFunction<String, String, Integer>() {
			@Override
			public Tuple2<String, Integer> call(String s) throws Exception {
				return new Tuple2<>(s, 1);
			}
		});

		JavaPairRDD<String, Integer> wordsCountRDD = wordAndOneRDD.reduceByKey(new Function2<Integer, Integer, Integer>() {
			@Override
			public Integer call(Integer integer, Integer integer2) throws Exception {
				return integer + integer2;
			}
		});

		JavaPairRDD<Integer, String> newWordsCountRDD = wordsCountRDD.mapToPair(new PairFunction<Tuple2<String, Integer>, Integer, String>() {
			@Override
			public Tuple2<Integer, String> call(Tuple2<String, Integer> stringIntegerTuple2) throws Exception {
				return stringIntegerTuple2.swap();
			}
		});
		JavaPairRDD<Integer, String> sortedRDD = newWordsCountRDD.sortByKey(false);
		JavaPairRDD<String, Integer> lastSortWordCoundRDD = sortedRDD.mapToPair(new PairFunction<Tuple2<Integer, String>, String, Integer>() {
			@Override
			public Tuple2<String, Integer> call(Tuple2<Integer, String> integerStringTuple2) throws Exception {
				return integerStringTuple2.swap();
			}
		});

		lastSortWordCoundRDD.foreach(new VoidFunction<Tuple2<String, Integer>>() {
			@Override
			public void call(Tuple2<String, Integer> t) throws Exception {
				System.out.println(t._1 + "\t" + t._2);
			}
		});

		javaSparkContext.stop();
	}
}
