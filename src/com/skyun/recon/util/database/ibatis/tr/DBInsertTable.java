package com.skyun.recon.util.database.ibatis.tr;

import java.sql.SQLException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.skyun.recon.util.database.ibatis.SqlMapInstance;
import com.skyun.recon.util.database.ibatis.vo.agentVo;
import com.skyun.recon.util.database.ibatis.vo.findlVo;
import com.skyun.recon.util.database.ibatis.vo.groupVo;
import com.skyun.recon.util.database.ibatis.vo.groupallVo;
import com.skyun.recon.util.database.ibatis.vo.locationVo;
import com.skyun.recon.util.database.ibatis.vo.netScheduleVo;
import com.skyun.recon.util.database.ibatis.vo.networkVo;
import com.skyun.recon.util.database.ibatis.vo.porfileVo;
import com.skyun.recon.util.database.ibatis.vo.reportVo;
import com.skyun.recon.util.database.ibatis.vo.schdelesVo;
import com.skyun.recon.util.database.ibatis.vo.schedule_locationVo;
import com.skyun.recon.util.database.ibatis.vo.subpathVo;
import com.skyun.recon.util.database.ibatis.vo.summaryVo;
import com.skyun.recon.util.database.ibatis.vo.targetVo;
import com.skyun.recon.util.database.ibatis.vo.userVo;

//Database Insert 
public class DBInsertTable {
	private static Logger logger = LoggerFactory.getLogger(DBInsertTable.class);
	private static SqlMapClient sqlMap = null;

	private int ret = 0;
	private String sexception;

	public DBInsertTable() {
		this.sqlMap = SqlMapInstance.getSqlMapInstance();
	}

	public DBInsertTable(SqlMapClient sqlMap) {
		this.sqlMap = sqlMap;
	}

	public void setDBInsertTable(String trid, Object obj) {

		try {
			if (obj instanceof groupallVo) {
				this.sqlMap.openSession().insert(trid, (groupallVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof targetVo) {
				this.sqlMap.openSession().insert(trid, (targetVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof locationVo) {
				this.sqlMap.openSession().insert(trid, (locationVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof findlVo) {
				this.sqlMap.openSession().insert(trid, (findlVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof agentVo) {
				this.sqlMap.openSession().insert(trid, (agentVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof networkVo) {
				this.sqlMap.openSession().insert(trid, (networkVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof schdelesVo) {
				this.sqlMap.openSession().insert(trid, (schdelesVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof schedule_locationVo) {
				this.sqlMap.openSession().insert(trid, (schedule_locationVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof porfileVo) {
				this.sqlMap.openSession().insert(trid, (porfileVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof subpathVo) {
				this.sqlMap.openSession().insert(trid, (subpathVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof summaryVo) {
				this.sqlMap.openSession().insert(trid, (summaryVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof userVo) {
				this.sqlMap.openSession().insert(trid, (userVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof groupVo) {
				this.sqlMap.openSession().insert(trid, (groupVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof reportVo) {
				this.sqlMap.openSession().insert(trid, (reportVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof netScheduleVo) {
				this.sqlMap.openSession().insert(trid, (netScheduleVo) obj);
				ret = 1;
				sexception = "OK";
			} else if (obj instanceof HashMap) {
				this.sqlMap.openSession().insert(trid, (HashMap<String, Object>) obj);
				ret = 1;
				sexception = "OK";
			} else {
				logger.info("unknown data type " + obj.getClass().getName());
			}

		} catch (SQLException e) {
			sexception = e.getMessage();
			ret = -1;
			logger.info("DB Error :____________________" + sexception);
		}

	}

	// parameter 없이 쿼리 실행 위해 method 생성 -- shlee // 20200827
	public void setDBInsertTable(String trid) {
		try {
			this.sqlMap.openSession().insert(trid);
			ret = 1;
			sexception = "OK";
		} catch (SQLException e) {
			sexception = e.getMessage();
			ret = -1;
			logger.info("DB Error :____________________" + sexception);
		}
	}
	public String getSexception() {
		return sexception;
	}

	public int getRet() {
		return ret;
	}
	
	public SqlMapClient getSqlclient() {
		return this.sqlMap;
	}

}
