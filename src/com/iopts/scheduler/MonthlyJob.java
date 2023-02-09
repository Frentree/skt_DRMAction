package com.iopts.scheduler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.reportVo;

public class MonthlyJob implements Runnable {
	private DBInsertTable tr = null;
	private static Logger logger = LoggerFactory.getLogger(MonthlyJob.class);
	
	public MonthlyJob() {
	}
	
	@Override
	public void run() {
		logger.info("start monthly job");
		List<reportVo> reportList = new ArrayList<>();
		try {
			tr = new DBInsertTable();
			
			int lastMonth_count = 0;
			lastMonth_count = (int) tr.getSqlclient().openSession().queryForObject("query.getMonthlyReportCount");
			
			if(lastMonth_count < 1) {
				logger.info("INSERT MONTHLY REPORT ON THE PAST MONTH.");
				
				reportList = tr.getSqlclient().openSession().queryForList("query.getMonthlyReport");
				logger.info("reportList.size(); :: " + reportList.size());
				
				for(int i=0; i<reportList.size(); i++) {
					tr.setDBInsertTable("insert.setMonthlyReport", reportList.get(i));
				}
			}
		} catch (SQLException e) {
			logger.error("query.getGroup fatal error !!!!!!!! \n" + e.getMessage());
		}
	}

}
