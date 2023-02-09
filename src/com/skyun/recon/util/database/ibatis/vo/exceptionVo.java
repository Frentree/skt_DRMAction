package com.skyun.recon.util.database.ibatis.vo;

public class exceptionVo {
	private String group_id;
	private String target_id;
	private String hash_id;
	private String keyid;
	private String value="OK";
	private String ok_user_no;

	public  exceptionVo() {

	}

	public String getGroup_id() {
		return group_id;
	}

	public void setGroup_id(String group_id) {
		this.group_id = group_id;
	}

	public String getTarget_id() {
		return target_id;
	}

	public void setTarget_id(String target_id) {
		this.target_id = target_id;
	}


	public String getHash_id() {
		return hash_id;
	}

	public void setHash_id(String hash_id) {
		this.hash_id = hash_id;
	}

	public String getKeyid() {
		return group_id+target_id+hash_id;
	}

	public void setKeyid(String keyid) {
		this.keyid =keyid;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getOk_user_no() {
		return ok_user_no;
	}

	public void setOk_user_no(String ok_user_no) {
		this.ok_user_no = ok_user_no;
	}

}
