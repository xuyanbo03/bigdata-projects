package com.awebone.hadoop_rpc;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

public class NameNodeClient {

	public static void main(String[] args) {
		
		
		try {
			MyServerProtocal proxy = RPC.getProxy(MyServerProtocal.class, 
					MyServerProtocal.versionID, 
					new InetSocketAddress("localhost", 9988), new Configuration());
			
			/**
			 * proxy.hello();
			 * 的底层，其实就是调用：
			 * 
			 * 服务器中的   setInstance这个参数对象中的hello方法
			 */
			proxy.hello();
			System.out.println(proxy.getName());
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
}
