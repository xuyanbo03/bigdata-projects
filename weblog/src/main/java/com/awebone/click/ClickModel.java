package com.awebone.click;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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

import com.awebone.bean.PageViewsBean;
import com.awebone.bean.VisitBean;
import com.awebone.bean.WebLogBean;

/**
 * map端： 相同的会话的数据  发送到  reduce 
 *		key： session 
 * 		value: 其他的字段  
			访问时间   url   step   外链   ip  
	reduce端：
		相同session的数据过来了
		按照step排序
		list 第一个开始
		list 最后一个结束
		封装 发送
 *
 */
public class ClickModel {
	static class ClickModelMapper extends Mapper<LongWritable, Text, Text, PageViewsBean>{
		Text mk = new Text();
		PageViewsBean pbean = new PageViewsBean();
		
		@Override
		protected void map(LongWritable key, Text value,
				Mapper<LongWritable, Text, Text, PageViewsBean>.Context context)
				throws IOException, InterruptedException {
			String[] fields = value.toString().split("\001");
			if (fields.length == 11){
				mk.set(fields[0]);
				int step=Integer.parseInt(fields[6]);
				pbean.set(fields[0], fields[1], fields[10], fields[3], fields[4],step,
						  fields[5], fields[9], fields[8], fields[7]);
				context.write(mk, pbean);
			}
		}
	}
	
	static class ClickModelReducer extends Reducer<Text, PageViewsBean, VisitBean, NullWritable>{
		VisitBean vb=new VisitBean();
		
		@Override
		protected void reduce(Text key, Iterable<PageViewsBean> values,
				Reducer<Text, PageViewsBean, VisitBean, NullWritable>.Context context)
				throws IOException, InterruptedException {
			ArrayList<PageViewsBean> list = new ArrayList<PageViewsBean>();
			for (PageViewsBean v:values){
				PageViewsBean pb = new PageViewsBean();
				try {
					BeanUtils.copyProperties(pb, v);
					list.add(pb);
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			Collections.sort(list, new Comparator<PageViewsBean>() {
				public int compare(PageViewsBean o1, PageViewsBean o2) {
					if(o1 == null || o2 == null){
						return 0;
					}
					return o1.getStep()-o2.getStep();
				}
			});
			
			//构造发送的对象
			vb.set(key.toString(), list.get(0).getRemote_addr(), 
					list.get(0).getTimestr(), list.get(list.size()-1).getTimestr(), 
					list.get(0).getRequest(), list.get(list.size()-1).getRequest(), 
					list.get(0).getReferal(), list.get(list.size()-1).getStep());
			context.write(vb, NullWritable.get());
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
		System.setProperty("HADOOP_USER_NAME", "hadoop");
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://myha/");
        Job job = Job.getInstance(conf);

        job.setJarByClass(ClickModel.class);

        job.setMapperClass(ClickModelMapper.class);
        job.setReducerClass(ClickModelReducer.class);
        
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(PageViewsBean.class);
        job.setOutputKeyClass(VisitBean.class);
        job.setOutputValueClass(NullWritable.class);

        FileInputFormat.setInputPaths(job, new Path("/weblog/click/stream/20200221"));
        FileOutputFormat.setOutputPath(job, new Path("/weblog/click/model/20200221"));

        boolean res = job.waitForCompletion(true);
        System.exit(res ? 0 : 1);
	}
}
