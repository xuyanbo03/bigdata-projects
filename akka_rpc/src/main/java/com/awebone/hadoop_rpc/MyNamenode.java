package com.awebone.hadoop_rpc;

import java.io.IOException;

import org.apache.hadoop.HadoopIllegalArgumentException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.ipc.RPC.Server;

public class MyNamenode {

	public static void main(String[] args) {
		
		
		try {
			
			/**
			 * new MyServerImpl().hello()   .getName()
			 */
			Server server = new RPC.Builder(new Configuration())
			.setProtocol(MyServerProtocal.class)
			.setInstance(new MyServerImpl())
			.setBindAddress("localhost")
			.setPort(9988)
			.build();
			
			
			server.start();
			System.out.println("SERVER START ......");
			
			
		} catch (HadoopIllegalArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
}
