package com.iopts.scheduler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.http.ParseException;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.iopts.scheduler.queue.QueueStaticPool;
import com.iopts.skyun.recon.vo.groupall.groupallCo;
import com.iopts.skyun.recon.vo.groupall.locationsCo;
import com.iopts.skyun.recon.vo.groupall.targetslCo;
import com.skyun.app.util.config.AppConfig;
import com.skyun.app.util.config.IoptsCurl;
import com.skyun.recon.util.database.ibatis.SqlMapInstance;
import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.groupallVo;
import com.skyun.recon.util.database.ibatis.vo.locationVo;
import com.skyun.recon.util.database.ibatis.vo.targetVo;

public class TargetCheckThread implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(TargetCheckThread.class);
	private DBInsertTable tr = null;
	private static BlockingQueue<targetVo> queue = null;
	private static SqlMapClient sqlMap = null;			// agent select 위해 추가 shlee // 20200828

	private static String customer_id = "";
//	private static int ap_no = 0;
	
	public TargetCheckThread() {
		this.sqlMap = SqlMapInstance.getSqlMapInstance();	// agent select 위해 추가 shlee // 20200828
		this.queue = new QueueStaticPool().getJobQueue();
	}

	@Override
	public void run() {
		this.customer_id = AppConfig.getProperty("config.customer");
		int delay = AppConfig.getPropertyInt("config.targetstatus.delay");
		try {
			while(true) {
				if("SKT".equals(customer_id)) {
					String str_ap_count = AppConfig.getProperty("config.recon.ap.count");
					int ap_count = ("".equals(str_ap_count)) ? 1 : Integer.parseInt(str_ap_count);
					
					for(int i=0; i<ap_count; i++) {
//						this.ap_no = i;
						executeRun(i);
					}
				} else {
					executeRun(0);
				}
				Thread.sleep((10 * 60 * 1000));
				
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
//		executeRun();
	}

	private void executeRun(int ap_no) {
		
		List<targetVo> reconTargetList = new ArrayList<>();
//		String user = AppConfig.getProperty("config.recon.user");
//		String pass = AppConfig.getProperty("config.recon.pawwsord");
		String user = (ap_no == 0) ? AppConfig.getProperty("config.recon.user") : AppConfig.getProperty("config.recon.user_"+(ap_no+1));
		String pass = (ap_no == 0) ? AppConfig.getProperty("config.recon.pawwsord") : AppConfig.getProperty("config.recon.pawwsord_"+(ap_no+1));
		String ip = (ap_no == 0) ? AppConfig.getProperty("config.recon.ip") : AppConfig.getProperty("config.recon.ip_"+(ap_no+1)) ;
		String port = AppConfig.getProperty("config.recon.port");
		String api_ver = AppConfig.getProperty("config.recon.api.version");
		
		String curlurl = String.format("-k -X GET -u %s:%s https://%s:%s/%s/groups/all?limit=30000", 
						user, pass, ip, port, api_ver);

		logger.info("Target Check curlurl [" + curlurl + "]");

		String[] array = curlurl.split(" ");
		
		int delay = AppConfig.getPropertyInt("config.targetstatus.delay");

		this.tr = new DBInsertTable();
		logger.info("Target Check__________________________:" + curlurl);
		
		String json_string = new IoptsCurl().opt(array).exec(null);
		
		try {
			// json_string = EntityUtils.toString(response.getEntity());

			json_string = new IoptsCurl().opt(array).exec(null);

			if (json_string == null || json_string.length() < 1 || json_string.contains("Resource not found.")) {
				logger.error("Data Null Check IP or ID: " + curlurl);
			} else {

				JSONArray temp1 = new JSONArray(json_string);

				
				
				for (int i = 0; i < temp1.length(); i++) {
					Gson gson = new Gson();
					groupallCo g = gson.fromJson(temp1.get(i).toString(), groupallCo.class);
					reconTargetList = groupall(g, ap_no);
				}
			}
			
			// TARGET 사용 정보 갱신 -- shlee // 20200827
			
			logger.info("reconTargetList.size :: " + reconTargetList.size());
			
			Map<String, Object> map = new HashMap<>();
			map.put("ap_no", ap_no);
			
			List<targetVo> dbTargetList = sqlMap.openSession().queryForList("query.getTargetUse", map);
			logger.info("dbTargetList.size :: " + dbTargetList.size());
			
			
			for (int i = 0; i < dbTargetList.size(); i++) {
				targetVo dbVo = dbTargetList.get(i);
				dbVo.setAp_no(ap_no);
				
				if(ap_no == 0) {
					logger.info(dbVo.getName());
				}
				boolean updateFlag = true;
				for (targetVo reconVo : reconTargetList) {
					if(dbVo.getGroup_id().equals(reconVo.getGroup_id()) && dbVo.getTarget_id().equals(reconVo.getTarget_id()) && dbVo.getName().equals(reconVo.getName())) {
						updateFlag = false;
						break;
					}
				}
				if(updateFlag) {
					logger.info("TARGET ["+dbVo.getTarget_id()+"]"+dbVo.getName()+" ==> N");
					tr.setDBInsertTable("update.setTargetUseToN", dbVo);
				}
			}
			
			if(ap_no == 0) {
				logger.info(reconTargetList.toArray().toString());
				for (targetVo reconVo : reconTargetList) {
					logger.info(reconVo.getName());
				}
			}
			
		} catch (ParseException e1) {
			e1.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}


	private List<targetVo> groupall(groupallCo g, int ap_no) throws Exception {
		List<targetVo> reconTargetList = new ArrayList<>();
		
		groupallVo v = new groupallVo();
		v.setValue(g);
		tr.setDBInsertTable("insert.setGroupAll", v);

		//

		if (g == null || g.getTargets() == null) {
			logger.info("Groupall Data is null ____");
			return reconTargetList;
		}

		if (g.getTargets().size() > 0) {
			for (targetslCo tco : g.getTargets()) {
				targetVo tvo = new targetVo();
				tvo.setValue(tco, v.getGroup_id());
				tvo.setAp_no(ap_no);
				
				// if (isThrerTarget(tvo) != true) {
				tr.setDBInsertTable("insert.setTarget", tvo);
				
				reconTargetList.add(tvo);
//				if(this.ap_no == 0) {
//					for (targetVo reconVo : reconTargetList) {
//						logger.info("test : " + reconVo.getName());
//					}
//				}
				// }
			}
		}

		if (g.getTargets().size() > 0) {

			for (targetslCo tco : g.getTargets()) {
				targetVo tvo = new targetVo();
				tvo.setValue(tco, v.getGroup_id());
				tvo.setAp_no(ap_no);

				if (tco.getLocations().size() > 0) {
					for (locationsCo lco : tco.getLocations()) {
						locationVo lvo = new locationVo();
						lvo.setValue(lco, v.getGroup_id(), tvo.getTarget_id());

						String key = v.getGroup_id() + tvo.getTarget_id() + lvo.getLocation_id();
						
						if (QueueStaticPool.getException_hash().get(key) == null) {
							tr.setDBInsertTable("insert.setLocation", lvo);
							try {
								this.queue.put(tvo);
							} catch (InterruptedException e) {
								logger.info("Target queue is full " + e.getLocalizedMessage());
							}
						} else {
							logger.info("Excpetion Define key :" + key);
						}
					}
				}
			}
		}
		
		return reconTargetList;
	}

}
