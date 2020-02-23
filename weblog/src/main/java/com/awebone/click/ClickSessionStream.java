package com.awebone.click;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.awebone.bean.WebLogBean;

/**
 * 抽取，转化 点击会话流的数据
 * map端：
 *		key： ip 
 *		value： 自定义类   字符串
 *	reduce：
 *		相同ip的数据 
 *		排序   按照访问时间  升序  排序
 *		计算相邻两个的时间差
 *		判断
 *
 */
public class ClickSessionStream {
	static class ClickSessionStreamMapper extends Mapper<LongWritable, Text, Text, WebLogBean>{
		Text mk = new Text();
		WebLogBean bean = new WebLogBean();
		
		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, WebLogBean>.Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] pre_datas = line.split("\001");
			if(pre_datas.length==9){
				bean.setValid(pre_datas[0].equals("true")?true:false);
				bean.setRemote_addr(pre_datas[1]);
				bean.setRemote_user(pre_datas[2]);
				bean.setTime_local(pre_datas[3]);
				bean.setRequest(pre_datas[4]);
				bean.setStatus(pre_datas[5]);
				bean.setBody_bytes_sent(pre_datas[6]);
				bean.setHttp_referer(pre_datas[7]);
				bean.setHttp_user_agent(pre_datas[8]);
				
				//过滤数据
				if(bean.isValid()){
					mk.set(bean.getRemote_addr());
					context.write(mk, bean);
				}
			}
		}
		
	}
		
	static class ClickSessionStreamReducer extends Reducer<Text, WebLogBean, Text, NullWritable>{
		Text rk = new Text();
		
		@Override
		protected void reduce(Text key, Iterable<WebLogBean> values,
				Reducer<Text, WebLogBean, Text, NullWritable>.Context context) throws IOException, InterruptedException {
			//相同ip的所有数据，循环遍历放在list中，按时间升序排序
			ArrayList<WebLogBean> list = new ArrayList<WebLogBean>();
			//reducer的坑：k和v都各自只有一个地址，因此要新建对象，再存在list中
			for (WebLogBean v:values){
				//新建对象
				WebLogBean bean = new WebLogBean();
				//将迭代器对象中的属性复制到新对象上
				try {
					BeanUtils.copyProperties(bean, v);
					list.add(bean);
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//按时间排序
			Collections.sort(list, new Comparator<WebLogBean>() {
				public int compare(WebLogBean o1, WebLogBean o2) {
					Date date1 = null;
					Date date2 = null;
					try {
						date1 = toDate(o1.getTime_local());
						date2 = toDate(o2.getTime_local());
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(date1==null || date2==null){
						return 0;
					}
					return date1.compareTo(date2);
				}
			});
			
			//遍历list，算停留时间，session，step=1
			int step = 1;
			UUID sessionid = UUID.randomUUID();
			for (int i = 0; i < list.size(); i++) {
				WebLogBean bean = list.get(i);
				//只有一个访问信息时，直接发送
				if(list.size()==1){
					rk.set(sessionid+"\001"+bean.getRemote_addr()+"\001"+bean.getRemote_user()+"\001"+
							bean.getTime_local()+"\001"+bean.getRequest()+"\001"+(60)+"\001"+step+"\001"+
							bean.getStatus()+"\001"+bean.getBody_bytes_sent()+"\001"+bean.getHttp_referer()+"\001"+
							bean.getHttp_user_agent());
					context.write(rk, NullWritable.get());
					sessionid = UUID.randomUUID();
					break;
				}
				
				//大于一个时，算时间差，当前条减去上一条时间
				if (i==0){
					continue;
				}
				try {
					long diffDate = diffDate(bean.getTime_local(), list.get(i-1).getTime_local());
					//判断时间差小于30min
					if(diffDate < 30*60*1000){
						WebLogBean lb = list.get(i-1);
						//输出上一条数据
						rk.set(sessionid+"\001"+lb.getRemote_addr()+"\001"+lb.getRemote_user()+"\001"+
								lb.getTime_local()+"\001"+lb.getRequest()+"\001"+(diffDate)/1000+"\001"+step+"\001"+
								lb.getStatus()+"\001"+lb.getBody_bytes_sent()+"\001"+lb.getHttp_referer()+"\001"+
								lb.getHttp_user_agent());
						context.write(rk, NullWritable.get());
						step++;
					}else{
						//大于30min，默认新的session，输出上一个会话的最后一个
						WebLogBean lsl = list.get(i-1);
						rk.set(sessionid+"\001"+lsl.getRemote_addr()+"\001"+lsl.getRemote_user()+"\001"+
								lsl.getTime_local()+"\001"+lsl.getRequest()+"\001"+(60)+"\001"+step+"\001"+
								lsl.getStatus()+"\001"+lsl.getBody_bytes_sent()+"\001"+lsl.getHttp_referer()+"\001"+
								lsl.getHttp_user_agent());
						context.write(rk, NullWritable.get());
						
						//step和session重新赋值
						step = 1;
						sessionid = UUID.randomUUID();
					}
					
					//输出最后一条
					if(i == list.size()-1){
						WebLogBean cb = list.get(i-1);
						rk.set(sessionid+"\001"+cb.getRemote_addr()+"\001"+cb.getRemote_user()+"\001"+
								cb.getTime_local()+"\001"+cb.getRequest()+"\001"+(60)+"\001"+step+"\001"+
								cb.getStatus()+"\001"+cb.getBody_bytes_sent()+"\001"+cb.getHttp_referer()+"\001"+
								cb.getHttp_user_agent());
						context.write(rk, NullWritable.get());
						sessionid = UUID.randomUUID();
					}
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		public static Date toDate(String time) throws ParseException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			Date date = sdf.parse(time);
			return date;
		}
		
		public static long diffDate(String date1,String date2) throws ParseException {
			Date d1 = toDate(date1);
			Date d2 = toDate(date2);
			return d1.getTime() - d2.getTime();
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
		System.setProperty("HADOOP_USER_NAME", "hadoop");
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://myha/");
        Job job = Job.getInstance(conf);

        job.setJarByClass(ClickSessionStream.class);

        job.setMapperClass(ClickSessionStreamMapper.class);
        job.setReducerClass(ClickSessionStreamReducer.class);
        
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(WebLogBean.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        FileInputFormat.setInputPaths(job, new Path("/weblog/pre/20200221"));
        FileOutputFormat.setOutputPath(job, new Path("/weblog/click/stream/20200221"));

        boolean res = job.waitForCompletion(true);
        System.exit(res ? 0 : 1);
	}
}
