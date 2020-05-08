package com.awebone.spark

import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.types._
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.sql.{SQLContext, SparkSession}

case class Movie(movieId: Int, title: String, genres: Seq[String])

case class User(userId: Int, gender: String, age: Int, occupation: Int, zip: String)

object DataProcess {
  //获取程序入口
  val sparkConf: SparkConf = new SparkConf()
  sparkConf.setAppName(DataProcess.getClass.getSimpleName)
  sparkConf.setMaster("local")
  val sc: SparkContext = new SparkContext(sparkConf)
  val sqlContext = new SQLContext(sc)

  import sqlContext.implicits._
  //  val spark:SparkSession = SparkSession.builder().appName("MyFirstSparkSQL").config("someKey", "someValue").master("local").getOrCreate()

  //Define parse function
  def parseMovie(str: String): Movie = {
    val fields = str.split("::")
    assert(fields.size == 3)
    Movie(fields(0).toInt, fields(1).toString, Seq(fields(2)))
  }

  def parseUser(str: String): User = {
    val fields = str.split("::")
    assert(fields.size == 5)
    User(fields(0).toInt, fields(1).toString, fields(2).toInt, fields(3).toInt, fields(4).toString)
  }

  def parseRating(str: String): Rating = {
    val fields = str.split("::")
    assert(fields.size == 4)
    Rating(fields(0).toInt, fields(1).toInt, fields(2).toInt)
  }

  //Rating analysis
  val ratingText = sc.textFile("file://D:\\workplace\\spark\\core\\src\\main\\resources\\ml-1m\\ratings.dat")
  ratingText.first()
  val ratingRDD = ratingText.map(parseRating).cache()
  println("Total number of ratings: " + ratingRDD.count())
  println("Total number of movies rated: " + ratingRDD.map(_.product).distinct().count())
  println("Total number of users who rated movies: " + ratingRDD.map(_.user).distinct().count())

  //Create DataFrames
  val ratingDF = ratingRDD.toDF
  //  val ratingDF = spark.createDataFrame(ratingRDD)
  val movieDF = sc.textFile("file://D:\\workplace\\spark\\core\\src\\main\\resources\\ml-1m\\movies.dat").map(parseMovie).toDF
  val userDF = sc.textFile("file://D:\\workplace\\spark\\core\\src\\main\\resources\\ml-1m\\users.dat").map(parseUser).toDF
  ratingDF.printSchema
  //  ratingDF.show
  movieDF.printSchema
  userDF.printSchema

  //注册成表
  ratingDF.registerTempTable("ratings")
  //  ratingDF.createOrReplaceTempView(“ratings”)
  movieDF.registerTempTable("movies")
  userDF.registerTempTable("users")

  //数据探索
  val rantingMovies = sqlContext.sql(
    """
      |select title,ramx,rmin,ucnt from
      |(select product, max(rating) as rmax, min(rating) as rmin, count(distinct user) as ucnt from ratings group by product) rantingsCNT
      |join movies on product=movieId
      |order by ucnt desc
    """.stripMargin)
  rantingMovies.show()

  val mostActiveUser = sqlContext.sql(
    """
      |select user,count(*) as cnt
      |from ratings group by user order by cnt desc limit 10
    """.stripMargin)
  mostActiveUser.show()

  val userRating = sqlContext.sql(
    """
      |select distinct title,rating
      |from ratings join movies on movieId=product
      |where user=4169 and rating>4
    """.stripMargin)
  userRating.show()

  //ALS model
  //数据切分
  val splitsData = ratingRDD.randomSplit(Array(0.8, 0.2), 0L)
  val trainingSet = splitsData(0).cache()
  val testSet = splitsData(0).cache()
  trainingSet.count()
  testSet.count()

  //构建模型
  val model = new ALS()
    .setRank(20)
    .setIterations(10)
    .run(trainingSet)

  //进行推荐
  val recomForTopUser = model.recommendProducts(4169, 5)
  val movieTitle = movieDF.rdd.map(x => (x(0), x(1))).collectAsMap
  val recomResult = recomForTopUser.map(rating => (movieTitle(rating.product), rating.rating)).foreach(println)

  //测试集预测
  val testUserProduct = testSet.map {
    case Rating(user, product, rating) => (user, product)
  }
  val testUserProductPredict = model.predict(testUserProduct)
  testUserProductPredict.take(10).mkString("\n")

  //模型评估
  val testSetPair = testSet.map {
    case Rating(user, product, rating) => ((user, product), rating)
  }
  val predictionsPair = testUserProductPredict.map {
    case Rating(user, product, rating) => ((user, product), rating)
  }

  val joinTestPredict = testSetPair.join(predictionsPair)
  val mae = joinTestPredict.map {
    case ((user, product), (ratingT, ratingP)) =>
      val err = ratingT - ratingP
      Math.abs(err)
  }.mean()
  val fp = joinTestPredict.filter {
    case ((user, product), (ratingT, ratingP)) =>
      (ratingT <= 1 & ratingP >= 4)
  }.count()

  //使用库进行评估
  val ratingTP = joinTestPredict.map {
    case ((user, product), (ratingT, ratingP)) =>
      (ratingP, ratingT)
  }
  val evalutor = new RegressionMetrics(ratingTP)
  evalutor.meanAbsoluteError
  evalutor.rootMeanSquaredError
}
