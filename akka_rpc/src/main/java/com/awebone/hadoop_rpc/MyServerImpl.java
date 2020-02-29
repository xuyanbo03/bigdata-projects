package com.awebone.hadoop_rpc;

public class MyServerImpl implements MyServerProtocal{

	@Override
	public void hello() {
		System.out.println("hi");
	}

	@Override
	public String getName() {
		return "mynamenode";
	}

}
