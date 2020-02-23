package com.awebone.pre;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.awebone.bean.WebLogBean;

public class WebLogParse {
	static SimpleDateFormat sdf1 = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss", Locale.US);
	static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	static Set<String> pages = new HashSet<String>();
	static {
		pages.add("/about");
		pages.add("/black-ip-list/");
		pages.add("/cassandra-clustor/");
		pages.add("/finance-rhive-repurchase/");
		pages.add("/hadoop-family-roadmap/");
		pages.add("/hadoop-hive-intro/");
		pages.add("/hadoop-zookeeper-intro/");
		pages.add("/hadoop-mahout-roadmap/");
	}

	public static WebLogBean parse(String line) throws ParseException {
		// 参数代表一行日志信息
		String[] log_datas = line.split(" ");
		if (log_datas.length >= 12) {
			String addr = log_datas[0];
			String user = log_datas[2];
			String local_time = log_datas[3];
			// 时间解析
			String format_time = sdf2.format(sdf1.parse(local_time.substring(1)));
			if (null == format_time || "".equals(format_time)) {
				format_time = "_invalid_";
			}
			String request = log_datas[6];
			String status = log_datas[8];
			String byte_sent = log_datas[9];
			String http_refer = log_datas[10];
			// 拼接浏览器对象
			StringBuffer sb = new StringBuffer();
			for (int i = 11; i < log_datas.length; i++) {
				sb.append(log_datas[i] + " ");
			}
			String user_agent = sb.substring(1, sb.length() - 2);

			WebLogBean bean = new WebLogBean(false, addr, user, format_time, request, status, byte_sent, http_refer,
					user_agent);
			// 判断数据有效性
			if ("_invalid_".equals(format_time)) {
				bean.setValid(false);
			}
			if (Integer.parseInt(bean.getStatus()) > 400) {
				bean.setValid(false);
			}
			if (pages.contains(bean.getRequest())) {
				bean.setValid(true);
			}
			return bean;
		}else{
			return null;
		}
	}

}
