package com.awebone.dmp.util

import org.apache.commons.lang3.StringUtils

object Utils {
    def parseInt(str:String):Int = {
        if(StringUtils.isEmpty(str)) {
            0
        } else {
            str.toInt
        }
    }

    def parseDouble(str:String):Double = {
        if(StringUtils.isEmpty(str)) {
            0.0
        } else {
            str.toDouble
        }
    }

    //yyyy-MM-dd hh:mm:ss--->hh
    def fmtHour(str: String):Option[String] = {
        if(StringUtils.isEmpty(str)) {
            None
        } else {
            Some(str.substring(str.indexOf(" ") + 1, str.indexOf(" ") + 3))
        }
    }

    //yyyy-MM-dd hh:mm:ss--->yyyy-MM-dd
    def fmtDate(str: String):Option[String] = {
        if(StringUtils.isEmpty(str)) {
            None
        } else {
            Some(str.substring(0, str.indexOf(" ")))
        }
    }

    //补全两位字符串
    def fulfill(str:String) = {
        if(str != null && str.length > 1) {
            str
        } else if(!"".equals(str) && str.length == 1){
            0 + "" + str
        } else {
            "other"
        }
    }
    //补全数字
    def fulfill(num:Int) = {
        if(num >= 0 && num < 10) {
            "0" + num
        } else {
            "" + num
        }
    }
}
