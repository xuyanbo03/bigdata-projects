package com.awebone.dmp.tags

import com.awebone.dmp.Logs
import com.awebone.dmp.constants.AdTagConstants

import scala.collection.mutable

/**
  * 地域标签（省标签格式：ZPxxx->1，地市标签格式：ZCxxx->1）xxx为省或市名称
  */
object AreaTag extends Tags {
    override def extractTag(logs: Logs) = {
        val areaMap = mutable.Map[String, Int]()
        if(logs.provincename != null) {
            areaMap.put(AdTagConstants.PREFIX_AD_PROVINCE_TAG + logs.provincename, 1)
        }
        if(logs.cityname != null) {
            areaMap.put(AdTagConstants.PREFIX_AD_CITY_TAG + logs.cityname, 1)
        }
        areaMap.toMap
    }
}
