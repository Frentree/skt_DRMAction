package com.iopts.scheduler;

import java.util.List;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.skyun.recon.util.database.ibatis.SqlMapInstance;
import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.pathVo;

public class drmJobs {
	private DBInsertTable tr = null;
	private static Logger logger = LoggerFactory.getLogger(drmJobs.class);
	private static SqlMapClient sqlMap = null;		

//	private static int ap_no = 0;
	
	public drmJobs() {
		this.sqlMap = SqlMapInstance.getSqlMapInstance();
		tr = new DBInsertTable();
		
		run();
	}

	public void run() {
		List<pathVo> list = null;
		List<pathVo> dList = null;
		
		try {
			list = this.sqlMap.queryForList("query.getDelDatePathList");
			dList = this.sqlMap.queryForList("query.getDRMJob");
			for (pathVo vo : list) {
				updateDelDate(vo);
			}
			for (pathVo vo : dList) {
				// registDRMJob(vo);
			}
		} catch (Exception e) {
			// TODO: handle exception
			logger.error(e.toString());
		}
		
	}
	
	private void updateDelDate(pathVo vo) {
		
		try {
		
			vo.setPath(vo.getPath ().replaceAll("\\\\\\\\", "\\\\"));
			tr.setDBInsertTable("update.setDelDate", vo);
			
			logger.info("test >> " + vo.toString());
		} catch (Exception e) {
			logger.error("mysql update error >>>> " + e.toString());
		}
		
	}
	
	private void registDRMJob(pathVo vo) {
		tr.setDBInsertTable("insert.setDRMJob", vo);
	}

}
