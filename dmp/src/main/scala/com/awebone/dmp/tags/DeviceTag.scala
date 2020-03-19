package com.awebone.dmp.tags

import com.awebone.dmp.Logs
import com.awebone.dmp.constants.AdTagConstants

import scala.collection.mutable

/**
  * 4）设备：操作系统|联网方式|运营商
    设备操作系统
        1	Android	D0001001
        2	IOS	D0001002
        3	Winphone	D0001003
        4	其他	D0001004
    设备联网方式
        WIFI	D0002001
        4G	D0002002
        3G	D0002003
        2G	D0002004
        NWTWORKOTHER	D0004004
    设备运营商方案
        移动	D0003001
        联通	D0003002
        电信	D0003003
        OPERATOROTHER	D0003004
  */
object DeviceTag extends Tags {
    override def extractTag(logs: Logs) = {
        val mMap = mutable.Map[String, Int]()
        //设备操作系统为：client
        if(logs.client != null) {
            mMap.put(AdTagConstants.PREFIX_AD_DEVICE_TAG + logs.client, 1)
        }
        //联网方式networkmannerid
        if(logs.networkmannerid != null) {
            mMap.put(AdTagConstants.PREFIX_AD_NETWORK_TAG + logs.networkmannerid, 1)
        }

        //设备运营商ispid
        if(logs.ispid != null) {
            mMap.put(AdTagConstants.PREFIX_AD_ISP_TAG + logs.ispid, 1)
        }
        mMap.toMap
    }
}
