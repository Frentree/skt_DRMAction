package com.iopts.scheduler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.iopts.skyun.recon.vo.groupall.agentCo;
import com.iopts.skyun.recon.vo.groupall.networkCo;
import com.skyun.app.util.config.AppConfig;
import com.skyun.app.util.config.IoptsCurl;
import com.skyun.recon.util.database.ibatis.SqlMapInstance;
import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.agentVo;
import com.skyun.recon.util.database.ibatis.vo.networkVo;

public class AgentCheckThread implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(AgentCheckThread.class);
	private DBInsertTable tr = null;
	private static SqlMapClient sqlMap = null;			// agent select 위해 추가 shlee // 20200828

	private static String customer_id = "";
//	private static int ap_no = 0;
	
	public AgentCheckThread() {
		this.sqlMap = SqlMapInstance.getSqlMapInstance();	// agent select 위해 추가 shlee // 20200828
	}

	@Override
	public void run() {
		this.customer_id = AppConfig.getProperty("config.customer");
		int delay = AppConfig.getPropertyInt("config.agentstatus.delay");
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
				Thread.sleep((delay * 60 * 1000));
				
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
//		executeRun();
	}

	private void executeRun(int ap_no) {
//		String user = AppConfig.getProperty("config.recon.user");
//		String pass = AppConfig.getProperty("config.recon.pawwsord");
		String user = (ap_no == 0) ? AppConfig.getProperty("config.recon.user") : AppConfig.getProperty("config.recon.user_"+(ap_no+1));
		String pass = (ap_no == 0) ? AppConfig.getProperty("config.recon.pawwsord") : AppConfig.getProperty("config.recon.pawwsord_"+(ap_no+1));
		String ip = (ap_no == 0) ? AppConfig.getProperty("config.recon.ip") : AppConfig.getProperty("config.recon.ip_"+(ap_no+1)) ;
		String port = AppConfig.getProperty("config.recon.port");
		String api_ver = AppConfig.getProperty("config.recon.api.version");
		
		String curlurl = String.format("-k -X GET -u %s:%s https://%s:%s/%s/agents?limit=50000", 
						user, pass, ip, port, api_ver);

		logger.info("Agent Check curlurl [" + curlurl + "]");

		String[] array = curlurl.split(" ");
		
		int delay = AppConfig.getPropertyInt("config.agentstatus.delay");

		this.tr = new DBInsertTable();
		logger.info("Agent Check__________________________:" + curlurl);
		
		String json_string = new IoptsCurl().opt(array).exec(null);
		
		if (json_string == null || json_string.length() < 1 || json_string.contains("Resource not found.")) {
			logger.error("Data Null Check IP or ID: " + curlurl);
		} else {
			
			try {
				
				JSONArray temp1 = new JSONArray(json_string);
				
				logger.info("getAgentUse");
				Map<String, Object> getMap = new HashMap<>();
				getMap.put("ap_no", ap_no);
				List<agentVo> dbAgentList = sqlMap.openSession().queryForList("query.getAgentUse", getMap);
				List<agentVo> reconAgentList = new ArrayList<>();
				
				logger.info("dbAgentList.size :: " + dbAgentList.size());
				
				for (int i = 0; i < temp1.length(); i++) {
					Gson gson = new Gson();
					agentCo a = gson.fromJson(temp1.get(i).toString(), agentCo.class);
					dbJobs(a, ap_no);
					
					agentVo av = new agentVo(a);
					reconAgentList.add(av);
				}
				
				logger.info("reconAgentList.size :: " + reconAgentList.size());
				
				for (int i = 0; i < dbAgentList.size(); i++) {
					agentVo dbVo = dbAgentList.get(i);
					dbVo.setAp_no(ap_no);
					
					boolean updateFlag = true;
					for (agentVo reconVo : reconAgentList) {
						if(dbVo.getAgent_id().equals(reconVo.getAgent_id()) && dbVo.getAgent_name().equals(reconVo.getAgent_name())) {
							updateFlag = false;
							break;
						}
					}
					if(updateFlag) {
						logger.info("AGENT ["+dbVo.getAgent_id()+"]"+dbVo.getAgent_name()+" ==> N");
						tr.setDBInsertTable("update.setAgentUseToN", dbVo);
					}
				}
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

	private void dbJobs(agentCo a, int ap_no) {

		// Agent���� ���
		agentVo av = new agentVo(a);
		av.setAp_no(ap_no);
		if (av.isAgent_connected() == true) {
			tr.setDBInsertTable("insert.setAgentStatus1", av);
		} else {
			tr.setDBInsertTable("insert.setAgentStatus2", av);
		}

		// ��Ʈ��ũ ���� ���
		if (a.getNets() != null) {
			for (networkCo nc : a.getNets()) {
				networkVo nv = new networkVo(nc, a.getId());
				nv.setAp_no(String.valueOf(ap_no));
				tr.setDBInsertTable("insert.setNetInfo", nv);
			}
		}
	}

}
