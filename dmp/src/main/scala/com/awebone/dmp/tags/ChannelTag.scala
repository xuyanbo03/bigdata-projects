package com.awebone.dmp.tags

import com.awebone.dmp.Logs
import com.awebone.dmp.constants.AdTagConstants

/**
  * 3）渠道（标签格式：CNxxxx->1）xxxx为渠道ID
  */
object ChannelTag extends Tags {
    override def extractTag(logs: Logs) = {
        if(logs.channelid == null) {
            Map[String, Int]()
        } else {
            Map[String, Int]((AdTagConstants.PREFIX_AD_CHANNEL_TAG + logs.channelid -> 1))
        }
    }
}
