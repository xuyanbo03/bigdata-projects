package com.awebone.bean;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class WebLogBean implements Writable {
	private boolean valid = true; // 判断数据是否合法
	private String remote_addr; // 记录客户端的ip地址
	private String remote_user; // 记录客户端用户名称,忽略属性"-"
	private String time_local; // 记录访问时间与时区
	private String request; // 记录请求的url与http协议
	private String status; // 记录请求状态；成功是200
	private String body_bytes_sent; // 记录发送给客户端文件主体内容大小
	private String http_referer; // 用来记录从那个页面链接访问过来的
	private String http_user_agent; // 记录客户浏览器的相关信息

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public String getRemote_addr() {
		return remote_addr;
	}

	public void setRemote_addr(String remote_addr) {
		this.remote_addr = remote_addr;
	}

	public String getRemote_user() {
		return remote_user;
	}

	public void setRemote_user(String remote_user) {
		this.remote_user = remote_user;
	}

	public String getTime_local() {
		return time_local;
	}

	public void setTime_local(String time_local) {
		this.time_local = time_local;
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getBody_bytes_sent() {
		return body_bytes_sent;
	}

	public void setBody_bytes_sent(String body_bytes_sent) {
		this.body_bytes_sent = body_bytes_sent;
	}

	public String getHttp_referer() {
		return http_referer;
	}

	public void setHttp_referer(String http_referer) {
		this.http_referer = http_referer;
	}

	public String getHttp_user_agent() {
		return http_user_agent;
	}

	public void setHttp_user_agent(String http_user_agent) {
		this.http_user_agent = http_user_agent;
	}

	public WebLogBean() {
		super();
	}

	public WebLogBean(boolean valid, String remote_addr, String remote_user, String time_local, String request,
			String status, String body_bytes_sent, String http_referer, String http_user_agent) {
		super();
		this.valid = valid;
		this.remote_addr = remote_addr;
		this.remote_user = remote_user;
		this.time_local = time_local;
		this.request = request;
		this.status = status;
		this.body_bytes_sent = body_bytes_sent;
		this.http_referer = http_referer;
		this.http_user_agent = http_user_agent;
	}

	@Override
	public String toString() {
		return valid + "\001" + remote_addr + "\001" + remote_user + "\001" + time_local + "\001" + request + "\001"
				+ status + "\001" + body_bytes_sent + "\001" + http_referer + "\001" + http_user_agent;
	}

	// 反序列化
	public void readFields(DataInput in) throws IOException {
		this.valid = in.readBoolean();
		this.remote_addr = in.readUTF();
		this.remote_user = in.readUTF();
		this.time_local = in.readUTF();
		this.request = in.readUTF();
		this.status = in.readUTF();
		this.body_bytes_sent = in.readUTF();
		this.http_referer = in.readUTF();
		this.http_user_agent = in.readUTF();
	}

	// 序列化
	public void write(DataOutput out) throws IOException {
		out.writeBoolean(valid);
		out.writeUTF(remote_addr);
		out.writeUTF(remote_user);
		out.writeUTF(time_local);
		out.writeUTF(request);
		out.writeUTF(status);
		out.writeUTF(body_bytes_sent);
		out.writeUTF(http_referer);
		out.writeUTF(http_user_agent);
	}

}
