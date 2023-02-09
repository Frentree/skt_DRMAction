package com.iopts.scheduler;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.skyun.recon.util.database.ibatis.vo.exceptionVo;
import com.skyun.recon.util.database.ibatis.vo.groupallVo;
import com.skyun.recon.util.database.ibatis.vo.locationVo;
import com.skyun.recon.util.database.ibatis.vo.pi_account_info;
import com.skyun.recon.util.database.ibatis.vo.targetVo;

public class ReconJobs implements Runnable {
	private DBInsertTable tr = null;
	private static Logger logger = LoggerFactory.getLogger(ReconJobs.class);
	private static BlockingQueue<targetVo> queue = null;
	private static SqlMapClient sqlMap = null;
	
	private static String customer_id = "";
//	private static int ap_no = 0;
	
	public ReconJobs() {
		this.sqlMap = SqlMapInstance.getSqlMapInstance();
		tr = new DBInsertTable();
		this.queue = new QueueStaticPool().getJobQueue();
		
		sethashException();

	}

	@Override
	public void run() {
		
		this.customer_id = AppConfig.getProperty("config.customer");
		
		if("SKT".equals(customer_id)) {
			String str_ap_count = AppConfig.getProperty("config.recon.ap.count");
			int ap_count = ("".equals(str_ap_count)) ? 1 : Integer.parseInt(str_ap_count);
			
			for(int i=0; i<ap_count; i++) {
//				this.ap_no = i;
				executeRun(i);
			}
		} else {
			executeRun(0);
		}
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

		logger.info("curlurl [" + curlurl + "]");

		String[] array = curlurl.split(" ");
		// HttpResponse response = curl(curlurl);

		String json_string;
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
					//tr.setDBInsertTable("update.setTargetUseToN", dbVo);
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

		// getgroupall();
		getOwnerlist();
	}

	private String getDate(String s) {

		Timestamp timestamp = new Timestamp(Long.parseLong(s) * 1000);
		Date date = new Date(timestamp.getTime());

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
		return simpleDateFormat.format(timestamp);

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

	private void getgroupall() {
		List<groupallVo> vols = new ArrayList<groupallVo>();
		try {
			vols = tr.getSqlclient().openSession().queryForList("query.getgroupall");
		} catch (SQLException e) {
			logger.error("query.getLogConfig ind fatal error !!!!!!!! \n" + e.getMessage());
		}
	}

	private boolean isThrerTarget(targetVo v) {
		boolean ret = false;

		List<groupallVo> vols = new ArrayList<groupallVo>();

		try {
			vols = tr.getSqlclient().openSession().queryForList("query.isThereTarget", v);
			if (vols != null && vols.size() > 0) {
				ret = true;
			}
		} catch (SQLException e) {
			logger.error("query.isThrerTarget ind fatal error !!!!!!!! \n" + e.getMessage());
		}

		return ret;
	}

	private void sethashException() {
		List<exceptionVo> vols = new ArrayList<exceptionVo>();

		HashMap<String, String> ehash = new HashMap<>();

		try {
			vols = tr.getSqlclient().openSession().queryForList("query.getExceptionList");
		} catch (SQLException e) {
			logger.error("query.getExceptionList ind fatal error !!!!!!!! \n" + e.getMessage());
		}

		for (exceptionVo v : vols) {
			ehash.put(v.getKeyid(), v.getValue());
		}

		QueueStaticPool.setException_hash(ehash);
	}
	
	private static void getOwnerlist() {		
		List<pi_account_info> vols = new ArrayList<pi_account_info>();
		try {
			vols = sqlMap.openSession().queryForList("query.getpiAccountInfo");
		} catch (SQLException e) {
			System.out.println("query.getpiAccountInfo ind fatal error !!!!!!!! \n" + e.getMessage());
		}
		
		AppConfig.account.clear();
		
		if(vols.size() > 0) {
			for(pi_account_info v:vols) {
				AppConfig.account.put(v.getTarget_id()+"_"+v.getAccount(),v.getUser_id());	
			}
		}
		
		logger.info("account ______________ list:"+AppConfig.account.size());

	}

}
