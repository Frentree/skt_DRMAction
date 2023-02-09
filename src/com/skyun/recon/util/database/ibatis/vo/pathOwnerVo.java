package com.skyun.recon.util.database.ibatis.vo;

public class pathOwnerVo {

	private String idx;
	private String path;
	private String target_id;
	private String target_name;
	private String team_name;
	private String user_no;
	private String user_name;
	
	public String getIdx() {
		return idx;
	}
	public void setIdx(String idx) {
		this.idx = idx;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getTarget_id() {
		return target_id;
	}
	public void setTarget_id(String target_id) {
		this.target_id = target_id;
	}
	public String getTarget_name() {
		return target_name;
	}
	public void setTarget_name(String target_name) {
		this.target_name = target_name;
	}
	public String getTeam_name() {
		return team_name;
	}
	public void setTeam_name(String team_name) {
		this.team_name = team_name;
	}
	public String getUser_no() {
		return user_no;
	}
	public void setUser_no(String user_no) {
		this.user_no = user_no;
	}
	public String getUser_name() {
		return user_name;
	}
	public void setUser_name(String user_name) {
		this.user_name = user_name;
	}
	
	@Override
	public String toString() {
		return "pathOwnerVo [idx=" + idx + ", path=" + path + ", target_id=" + target_id + ", target_name="
				+ target_name + ", team_name=" + team_name + ", user_no=" + user_no + ", user_name=" + user_name + "]";
	}
	
	
}
