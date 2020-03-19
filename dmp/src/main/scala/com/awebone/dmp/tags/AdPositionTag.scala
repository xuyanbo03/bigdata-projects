package com.awebone.dmp.tags

import com.awebone.dmp.Logs
import com.awebone.dmp.constants.AdTagConstants
import com.awebone.dmp.util.Utils

import scala.collection.mutable

/**
  * 标签一：
1）广告位类型（标签格式：LC03->1或者LC16->1）xx为数字，小于10 补0
  */
object AdPositionTag extends Tags {

    override def extractTag(logs: Logs) = {
        val map = mutable.Map[String, Int]()
        val adspacetype = Utils.fulfill(logs.adspacetype)
        map.put(AdTagConstants.PREFIX_AD_SPACE_TAG + "" + adspacetype, 1)
        map.toMap
    }
}
