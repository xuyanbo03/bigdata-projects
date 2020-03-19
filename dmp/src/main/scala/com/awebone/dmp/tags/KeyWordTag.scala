package com.awebone.dmp.tags

import com.awebone.dmp.Logs
import com.awebone.dmp.constants.AdTagConstants

import scala.collection.mutable

/**
  * 5）关键词（标签格式：Kxxx->1）xxx为关键字。
  * 关键词个数不能少于3个字符，且不能超过8个字符；
  * 关键字中如包含”|”,则分割成数组，转化成多个关键字标签
    “麻辣小龙虾|麻辣香锅|与神对话|家”
  */
object KeyWordTag extends Tags {
    override def extractTag(logs: Logs) = {
        val map = mutable.Map[String, Int]()
        if(logs.keywords != null) {
            val kws = logs.keywords.split("\\|")
            for (kw <- kws) {
                if(kw.length >= 3 && kw.length <= 8) {
                    map.put(AdTagConstants.PREFIX_AD_KEYWORD_TAG + kw, 1)
                }
            }
        }
        map.toMap
    }
}
