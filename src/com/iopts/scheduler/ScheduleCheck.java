package com.iopts.scheduler;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.iopts.scheduler.queue.QueueStaticPool;
import com.iopts.skyun.recon.vo.groupall.scheduleCo;
import com.skyun.app.util.config.AppConfig;
import com.skyun.app.util.config.IoptsCurl;
import com.skyun.recon.util.database.ibatis.SqlMapInstance;
import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.exceptionVo;
import com.skyun.recon.util.database.ibatis.vo.schdelesVo;
import com.skyun.recon.util.database.ibatis.vo.targetVo;

public class ScheduleCheck implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(ScheduleCheck.class);
	private static BlockingQueue<targetVo> queue = null;

	private String gid = "";
	private String tid = "";
	
	private List<exceptionVo> exceptionList;
	
	private static String customer_id = "";
//	private static int ap_no = 0;
	
	DBInsertTable tr = new DBInsertTable();
	private static SqlMapClient sqlMap = null;

	
	public ScheduleCheck() {
		this.queue = new QueueStaticPool().getJobQueue();
	}
	
	@Override
	public void run() {
		logger.info("start scheduleCheck job");

		this.sqlMap = SqlMapInstance.getSqlMapInstance();
		
		this.customer_id = AppConfig.getProperty("config.customer");
		
		if("SKT".equals(customer_id)) {
			String str_ap_count = AppConfig.getProperty("config.recon.ap.count");
			int ap_count = ("".equals(str_ap_count)) ? 1 : Integer.parseInt(str_ap_count);
			logger.info("ap_count :: " + ap_count);
			
			for(int i=0; i<ap_count; i++) {
//				this.ap_no = i;
				executeRun(i);
			}
		} else {
			executeRun(0);
		}

	} // run

	private void executeRun(int ap_no) {
//		String user = AppConfig.getProperty("config.recon.user");
//		String pass = AppConfig.getProperty("config.recon.pawwsord");
		// String ip = AppConfig.getProperty("config.recon.ip");
		String user = (ap_no == 0) ? AppConfig.getProperty("config.recon.user") : AppConfig.getProperty("config.recon.user_"+(ap_no+1));
		String pass = (ap_no == 0) ? AppConfig.getProperty("config.recon.pawwsord") : AppConfig.getProperty("config.recon.pawwsord_"+(ap_no+1));
		String ip = (ap_no == 0) ? AppConfig.getProperty("config.recon.ip") : AppConfig.getProperty("config.recon.ip_"+(ap_no+1)) ;
		String port = AppConfig.getProperty("config.recon.port");
		String api_ver = AppConfig.getProperty("config.recon.api.version");
		String start_date = "";
		String end_date = "";

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -1);
		start_date = sdf.format(cal.getTime());
		end_date = sdf.format(new Date());		
		String curlurl = String.format("-k -X GET -u %s:%s 'https://%s:%s/%s/schedules?details=true&completed=true&cancelled=true&stopped=true&failed=true&deactivated=true'",
				user, pass, ip, port, api_ver);

		logger.info("SchedulerCheck curlurl [" + curlurl + "]");

		String[] array = curlurl.split(" ");

		tr = new DBInsertTable();
		
		String json_string;
		json_string = new IoptsCurl().opt(array).exec(null);

		if (json_string == null || json_string.length() < 1 || json_string.contains("Resource not found.")) {
			logger.error("Data Null Check IP or ID: " + curlurl);

		} else {

			JSONArray temp1 = new JSONArray(json_string);

			List<String> list = new ArrayList<String>();
//			logger.info("\n\n\n\nschedule length :: " + temp1.length());
//			logger.info("ap_no :: " + this.ap_no);
			
			for (int i = 0; i < temp1.length(); i++) {
				Gson gson = new Gson();
				scheduleCo g = gson.fromJson(temp1.get(i).toString(), scheduleCo.class);
				g.setAp_no(ap_no);
				
				try {
					if( "completed".equals(g.getStatus()) || "stopped".equals(g.getStatus()) ) {
//						logger.info("["+g.getId()+"] "+g.getLabel()+" - " + g.getStatus());
						
						ArrayList<schdelesVo> vo_list = (ArrayList<schdelesVo>) tr.getSqlclient().openSession().queryForList("query.getUpdateList", g);
//						logger.info("size :: " + vo_list.size());
						
						for(schdelesVo vo : vo_list) {
							String target_id = vo.getSchedule_target_id();
							
							String[] target_ids = target_id.split("\\,");
							for(String t_id : target_ids) {
								list.add(t_id);
								
							}
						}
					}
					
					if(((int)tr.getSqlclient().openSession().queryForObject("query.getScheduleCount", g)) < 1){
						schdelesVo v = new schdelesVo(g);
						String[] target_ids = v.getSchedule_target_id().split("\\,");
						for(String t_id : target_ids) {
							list.add(t_id);
							
						}
					}
					
					// PIC 스케줄 데이터 업데이트
					ReconSchedulerJobs schedulerJobs = new ReconSchedulerJobs();
					g.setAp_no(ap_no);
					schedulerJobs.dbjobs(g,ap_no);
				
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} // temp for 
			
			
			
		} // else
	}

}
