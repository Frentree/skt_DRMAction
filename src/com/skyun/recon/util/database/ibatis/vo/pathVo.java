package com.skyun.recon.util.database.ibatis.vo;

public class pathVo {
	private String hostname;
	private String path;
	private String type;
	private String regdate;
	
	public pathVo() {
		// TODO Auto-generated constructor stub
	}

	public pathVo(String hostname, String path, String type, String regdate) {
		this.hostname = hostname;
		this.path = path;
		this.type = type;
		this.regdate = regdate;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getRegdate() {
		return regdate;
	}

	public void setRegdate(String regdate) {
		this.regdate = regdate;
	}

	@Override
	public String toString() {
		return "pathVo [hostname=" + hostname + ", path=" + path + ", type=" + type + ", regdate=" + regdate + "]";
	}
	
}
