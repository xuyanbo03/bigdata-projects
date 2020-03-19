package com.awebone.dmp.tags

import com.awebone.dmp.Logs

/**
  * 用户提取标签的特质
  */
trait Tags {

    def extractTag(logs:Logs):Map[String, Int]
}
