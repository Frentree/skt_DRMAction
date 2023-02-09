package com.iopts.scheduler;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.iopts.skyun.recon.vo.groupall.scheduleCo;
import com.iopts.skyun.recon.vo.groupall.schlocationCo;
import com.iopts.skyun.recon.vo.groupall.schtargetCo;
import com.skyun.app.util.config.AppConfig;
import com.skyun.app.util.config.IoptsCurl;
import com.skyun.recon.util.database.ibatis.SqlMapInstance;
import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.netScheduleVo;
import com.skyun.recon.util.database.ibatis.vo.pi_account_info;
import com.skyun.recon.util.database.ibatis.vo.schdelesVo;
import com.skyun.recon.util.database.ibatis.vo.schedule_locationVo;

public class ReconSchedulerJobs implements Runnable {
	private DBInsertTable tr = null;
	private static Logger logger = LoggerFactory.getLogger(ReconSchedulerJobs.class);
	private static SqlMapClient sqlMap = null;		

	private static String customer_id = "";
//	private static int ap_no = 0;
	
	public ReconSchedulerJobs() {
		this.sqlMap = SqlMapInstance.getSqlMapInstance();
	}

	@Override
	public void run() {

		this.customer_id = AppConfig.getProperty("config.customer");
		
		if("SKT".equals(customer_id)) {
			String str_ap_count = AppConfig.getProperty("config.recon.ap.count");
			int ap_count = ("".equals(str_ap_count)) ? 1 : Integer.parseInt(str_ap_count);
			System.out.println("ap Count >> " + ap_count);
			logger.info("ap Count >> " + ap_count);
			
			for(int i=0; i<ap_count; i++) {
//				this.ap_no = i;
				executeRun(i);
			}
		} else {
			executeRun(0);
		}
		

	}

	private void executeRun(int ap_no) {
//		String user = AppConfig.getProperty("config.recon.user");
//		String pass = AppConfig.getProperty("config.recon.pawwsord");
		//String ip = AppConfig.getProperty("config.recon.ip");
		String user = (ap_no == 0) ? AppConfig.getProperty("config.recon.user") : AppConfig.getProperty("config.recon.user_"+(ap_no+1));
		String pass = (ap_no == 0) ? AppConfig.getProperty("config.recon.pawwsord") : AppConfig.getProperty("config.recon.pawwsord_"+(ap_no+1));
		String ip = (ap_no == 0) ? AppConfig.getProperty("config.recon.ip") : AppConfig.getProperty("config.recon.ip_"+(ap_no+1)) ;
		String port = AppConfig.getProperty("config.recon.port");
		String api_ver = AppConfig.getProperty("config.recon.api.version");
		String start_date = "";
		String end_date = "";

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		
		// 기간 입력
		int startDateInput = Integer.parseInt(AppConfig.getProperty("config.reconSchedule.startTime"));
		
		Calendar cal = Calendar.getInstance();
		String curlurl = "";
		
		// 전체 기간 업데이트
		if(startDateInput == 0 ) {
			curlurl = String.format("-k -X GET -u %s:%s 'https://%s:%s/%s/schedules?details=true&completed=true&cancelled=true&stopped=true&failed=true&deactivated=true&limit=5000000'",
					user, pass, ip, port, api_ver);
		} else {
			cal.add(Calendar.DATE, (-1 * startDateInput));
			start_date = sdf.format(cal.getTime());
			end_date = sdf.format(new Date());		
			
			curlurl = String.format("-k -X GET -u %s:%s 'https://%s:%s/%s/schedules?details=true&completed=true&cancelled=true&stopped=true&failed=true&deactivated=true&limit=5000000&start_date=%s&end_date=%s'",
					user, pass, ip, port, api_ver,start_date,end_date);
		}
		
		
		logger.info("ReconSchedulerJobs curlurl [" + curlurl + "]");

		String[] array = curlurl.split(" ");

		tr = new DBInsertTable();

		String json_string;
		json_string = new IoptsCurl().opt(array).exec(null);

		if (json_string == null || json_string.length() < 1 || json_string.contains("Resource not found.")) {
			logger.error("Data Null Check IP or ID: " + curlurl);

		} else {

			JSONArray temp1 = new JSONArray(json_string);
			for (int i = 0; i < temp1.length(); i++) {
				Gson gson = new Gson();
				scheduleCo g = gson.fromJson(temp1.get(i).toString(), scheduleCo.class);
				g.setAp_no(ap_no);
				dbjobs(g, ap_no);
			}
		}
	}

	public void dbjobs(scheduleCo g, int ap_no) {
		schdelesVo v = new schdelesVo(g);
		v.setAp_no(ap_no);
		
		if(ap_no != 0) {
			try {
				List<netScheduleVo> list = null;
				
				Map<String, Object> getMap = new HashMap<>();
				getMap.put("target_id", v.getSchedule_target_id());
				getMap.put("ap_no", v.getAp_no());
				String drm_status = "";
				
				List<netScheduleVo> drm_status_list = sqlMap.openSession().queryForList("query.getDrmStatus", v);
				
				for(netScheduleVo nVo : drm_status_list) {
					nVo.setTarget_id(v.getSchedule_target_id());
					nVo.setAp_no(v.getAp_no());
				}
				
				if(drm_status_list.size() > 0) {
					drm_status = drm_status_list.get(0).getDrm_status();
				}
				v.setDrm_status(drm_status);
				
				logger.info("ReconSchedulerJobs Data" + v.toString());
				
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (NullPointerException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		
		
		// target_id 나 target_name 이 null 으로 들어올 경우 10번 루프 
		for(int i=0; i<10; i++) {
			if(!"".equals(v.getSchedule_target_id()) && v.getSchedule_target_id() != null && !"".equals(v.getSchedule_target_name()) && v.getSchedule_target_name() != null) {
				// id와 name에 빈값이 아닐경우 insert 후 루프 종료
				try {
					tr = new DBInsertTable();
					
					if(g.getAp_no() == 0) {
						tr.setDBInsertTable("insert.setSchedule1", v);
					} else {
						v.setAp_no(g.getAp_no());
						tr.setDBInsertTable("insert.setSchedule", v);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			}
		}
		
		// scanning 상태가 되면 pi_scan_schedule 테이블의 active_status 02로 설정된 값을 01로 변경
		/*if(v.getSchedule_status().equals("scanning")) {
			tr.setDBInsertTable("update.setSchedule", v);
		}*/

		if (g.getTargets() != null) {
			for (schtargetCo t : g.getTargets()) {
				if (t.getLocations() != null) {
					for (schlocationCo sloc : t.getLocations()) {
						schedule_locationVo slocvo = new schedule_locationVo(sloc, g.getId(), t.getId());
						try {
							if(g.getAp_no() == 0) {
								tr.setDBInsertTable("insert.setSchLocation1", slocvo);
							} else {
								slocvo.setAp_no(g.getAp_no());
								tr.setDBInsertTable("insert.setSchLocation", slocvo);
							}
						}catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
}
