package com.iopts.scheduler;

import org.apache.http.ParseException;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.iopts.skyun.recon.vo.groupall.profileCo;
import com.skyun.app.util.config.AppConfig;
import com.skyun.app.util.config.IoptsCurl;
import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.porfileVo;

//2019년 3월 22일 추가 
//pi_datatypes테이블에 입력함
public class ReconProfileJob implements Runnable {
	private DBInsertTable tr = null;
	private static Logger logger = LoggerFactory.getLogger(ReconProfileJob.class);
	
	private static String customer_id = "";
//	private static int ap_no = 0;

	public ReconProfileJob() {
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
		// TODO Auto-generated method stub
//		String user = AppConfig.getProperty("config.recon.user");
//		String pass = AppConfig.getProperty("config.recon.pawwsord");
		//String ip = AppConfig.getProperty("config.recon.ip");
		String user = (ap_no == 0) ? AppConfig.getProperty("config.recon.user") : AppConfig.getProperty("config.recon.user_"+(ap_no+1));
		String pass = (ap_no == 0) ? AppConfig.getProperty("config.recon.pawwsord") : AppConfig.getProperty("config.recon.pawwsord_"+(ap_no+1));
		String ip = (ap_no == 0) ? AppConfig.getProperty("config.recon.ip") : AppConfig.getProperty("config.recon.ip_"+(ap_no+1)) ;
		String port = AppConfig.getProperty("config.recon.port");
		String api_ver = AppConfig.getProperty("config.recon.api.version");
		
		/*String curlurl = String.format("-k -X GET -u %s:%s https://%s:%s/beta/datatypes/profiles?details=true", AppConfig.getProperty("config.recon.user"),
				AppConfig.getProperty("config.recon.pawwsord"), AppConfig.getProperty("config.recon.ip"), AppConfig.getProperty("config.recon.port"));*/
		String curlurl = String.format("-k -X GET -u %s:%s https://%s:%s/%s/datatypes/profiles?details=true", 
				user, pass, ip, port, api_ver);
		
		logger.info("curlurl [" + curlurl + "]");

		// HttpResponse response = curl(curlurl);

		String[] array = curlurl.split(" ");

		tr = new DBInsertTable();

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
					profileCo g = gson.fromJson(temp1.get(i).toString(), profileCo.class);
					dbjobs(g, ap_no);
				}
			}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
	}

	private void dbjobs(profileCo g, int ap_no) {
		// Agent상태 등록

		porfileVo v = new porfileVo(g);

		if (!(g.getOwner().trim().equals("0"))) {
			v.setAp_no(ap_no);
			tr.setDBInsertTable("insert.setProfile", v);
		}
	}
}
