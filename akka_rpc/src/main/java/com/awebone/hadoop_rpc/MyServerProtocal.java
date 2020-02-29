package com.awebone.hadoop_rpc;

public interface MyServerProtocal {
	
	long versionID = 12345678L;

	void hello();
	
	String getName();
}
