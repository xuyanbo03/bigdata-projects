package com.awebone.dmp.tags

import com.awebone.dmp.Logs
import com.awebone.dmp.constants.AdTagConstants

object AppTag extends Tags {
    override def extractTag(logs: Logs) = {
        val map = Map[String, Int]((AdTagConstants.PREFIX_AD_APP_TAG + logs.appname -> 1))
        map
    }
}
