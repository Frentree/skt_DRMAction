package com.skyun.recon.util.database.ibatis.vo;

public class netScheduleVo {
	
	private String target_id;
	private int ap_no;
	private String drm_status;
	
	public String getTarget_id() {
		return target_id;
	}
	public void setTarget_id(String target_id) {
		this.target_id = target_id;
	}
	public int getAp_no() {
		return ap_no;
	}
	public void setAp_no(int ap_no) {
		this.ap_no = ap_no;
	}
	public String getDrm_status() {
		return drm_status;
	}
	public void setDrm_status(String drm_status) {
		this.drm_status = drm_status;
	}
	
	@Override
	public String toString() {
		return "netScheduleVo [target_id=" + target_id + ", ap_no=" + ap_no + ", drm_status=" + drm_status + "]";
	}

}
