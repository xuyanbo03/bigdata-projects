package com.awebone.pre;

import java.io.IOException;
import java.text.ParseException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.awebone.bean.WebLogBean;

//对原始数据进行预处理
public class WebLogPreProcess {
	/**
	 * @author Awebone 
	 * map端： 
	 * 一行数据--- 一条日志--- hive一条数据 
	 * 切分 封装对象 发送 写出hdfs 
	 * 		key：null
	 * 		value：自定义对象
	 */
	static class WebLogPreProcessMapper extends Mapper<LongWritable, Text, NullWritable, WebLogBean> {
		@Override
		protected void map(LongWritable key, Text value,
				Mapper<LongWritable, Text, NullWritable, WebLogBean>.Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			try {
				WebLogBean webLogBean = WebLogParse.parse(line);
				if (webLogBean != null) {
					context.write(NullWritable.get(), webLogBean);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws ClassNotFoundException, IOException, InterruptedException {
		System.setProperty("HADOOP_USER_NAME", "hadoop");
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://myha/");
        Job job = Job.getInstance(conf);

        job.setJarByClass(WebLogPreProcess.class);

        job.setMapperClass(WebLogPreProcessMapper.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(WebLogBean.class);

        FileInputFormat.setInputPaths(job, new Path("/weblog/20200221"));
        FileOutputFormat.setOutputPath(job, new Path("/weblog/pre/20200221"));

        //不需要  设置为0 
        job.setNumReduceTasks(0);

        boolean res = job.waitForCompletion(true);
        System.exit(res ? 0 : 1);
	}
}
