package com.awebone.flink.project

import java.text.SimpleDateFormat
import java.util.{Date, Properties}

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.util.Random

object MockKafkaProducer {

  private def getLevels() = {
    val levels = Array[String]("M","E")

    levels(new Random().nextInt(levels.length))
  }

  private def getIps() = {
    val ips = Array[String]("233.104.18.110",
    "113.101.75.194",
    "27.17.127.135",
    "185.225.139.16",
    "112.1.66.34",
    "175.148.211.190",
    "183.227.58.21",
    "59.83.198.84",
    "117.28.38.28",
    "117.59.39.169")

    ips(new Random().nextInt(ips.length))
  }

  private def getDomains() = {
    val domains = Array[String]("v1.awebone.com", "v2.awebone.com", "v3.awebone.com", "v4.awebone.com", "vmi.awebone.com")

    domains(new Random().nextInt(domains.length))
  }

  private def getTraffic() = new Random().nextInt(10000)

  def main(args: Array[String]): Unit = {
    val properties: Properties = new Properties()
    properties.setProperty("bootstrap.servers","hadoop01:9092,hadoop02:9092,hadoop03:9092,hadoop04:9092")
    properties.setProperty("zookeeper.connect", "hadoop02:2181,hadoop03:2181,hadoop01:2181/kafka") //声明zk
//    properties.put("metadata.broker.list", "hadoop04:9092") // 声明kafka broker
    properties.setProperty("key.serializer", classOf[StringSerializer].getName)
    properties.setProperty("value.serializer", classOf[StringSerializer].getName)

    val producer = new KafkaProducer[String, String](properties)
    val topic = "cdnlog"

    while (true){
      val builder = new StringBuilder()
      builder.append("cdnlog").append("\t")
        .append("CN").append("\t")
        .append(getLevels()).append("\t")
        .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\t")
        .append(getIps()).append("\t")
        .append(getDomains()).append("\t")
        .append(getTraffic()).append("\t")

      println(builder.toString())
      val pr = new ProducerRecord[String, String](topic, builder.toString())
      producer.send(pr)
      Thread.sleep(2000)
    }
  }
}
