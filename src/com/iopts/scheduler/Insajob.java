package com.iopts.scheduler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skyun.app.util.config.AppConfig;
import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.groupVo;
import com.skyun.recon.util.database.ibatis.vo.userVo;

public class Insajob implements Runnable {
	private DBInsertTable tr = null;
	private static Logger logger = LoggerFactory.getLogger(Insajob.class);
	
	public Insajob() {
	}
	
	@Override
	public void run() {
		// 시스템운영팀, 시스템개발팀, 정보보호팀, 정보기술담당, 법규준수팀
		List<groupVo> group_list = new ArrayList<>();
		try {
			tr = new DBInsertTable();
			group_list = tr.getSqlclient().openSession().queryForList("query.getGroup");
		} catch (SQLException e) {
			logger.error("query.getGroup fatal error !!!!!!!! \n" + e.getMessage());
		}
		
		for(groupVo group : group_list) {
			getInsaInfo_AD(group);
		}
		
		// 상급자 입력
		for(groupVo group : group_list) {
			setBoss_name(group);
		}

	}
	
	private void setBoss_name(groupVo group) {
		tr.setDBInsertTable("update.setBossName", group);
	}
	
	private void getInsaInfo_AD(groupVo group) {
		String adIp = AppConfig.getProperty("config.ad.ip");
		String adPort = AppConfig.getProperty("config.ad.port");
		String adAccount = AppConfig.getProperty("config.ad.user");
		String adPassword = AppConfig.getProperty("config.ad.password");
		
		Hashtable<String, String> env = new Hashtable<>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, "ldap://"+adIp+":"+adPort);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, adAccount);
		env.put(Context.SECURITY_CREDENTIALS, adPassword);
		
		try {
			tr = new DBInsertTable();
			DirContext context = new InitialDirContext(env);
			
			SearchControls searcher = new SearchControls();
			searcher.setSearchScope(SearchControls.SUBTREE_SCOPE);
			
			String filter = "(&(objectClass=*)(department="+group.getTeam_name()+"))";
			
			NamingEnumeration results = context.search("OU=MERITZ_Users,DC=meritz,DC=dom", filter, searcher);
			
			while(results.hasMore()) {
				SearchResult result = (SearchResult) results.next();
				Attributes attrs = result.getAttributes();
				if(attrs.get("personalTitle") != null) {
					userVo user = new userVo();
					if("정보기술담당".equals(group.getTeam_name())) {
						user.setUser_no(attrs.get("sAMAccountName").toString().replaceAll("sAMAccountName: ", ""));
						user.setUser_name(attrs.get("displayName").toString().replaceAll("displayName: ", ""));
						user.setUser_email(attrs.get("mail").toString().replaceAll("mail: ", ""));
						user.setInsa_code(group.getInsa_code());
						user.setInsa_name(group.getTeam_name());
						user.setJikguk(attrs.get("personalTitle").toString().replaceAll("personalTitle: ", ""));
						user.setJikwee(setPositionCode(user.getJikguk()));
						user.setAccess_ip("");
						user.setUser_grade("9");
					} else {
						user.setUser_no(attrs.get("sAMAccountName").toString().replaceAll("sAMAccountName: ", ""));
						user.setUser_name(attrs.get("displayName").toString().replaceAll("displayName: ", ""));
						user.setUser_email(attrs.get("mail").toString().replaceAll("mail: ", ""));
						user.setInsa_code(group.getInsa_code());
						user.setInsa_name(group.getTeam_name());
						user.setJikguk(setPosition(attrs.get("title").toString().replaceAll("title: ", ""), attrs.get("personalTitle").toString().replaceAll("personalTitle: ", "")));
						user.setJikwee(setPositionCode(user.getJikguk()));
						user.setAccess_ip("");

						if("정보보호팀".equals(group.getTeam_name())) {
							user.setUser_grade("9");
						} else {
							if("팀장".equals(user.getJikguk())) {
								user.setUser_grade("2");
							} else {
								user.setUser_grade("0");
							}
						}
						
					}
					
					tr.setDBInsertTable("insert.setUser", user);
					tr.setDBInsertTable("insert.setAccountInfo", user);
				}
			}
			
		} catch (AuthenticationException e) {
			logger.error("LOGIN 실패 ["+ adAccount +"]");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String setPosition(String title, String personalTitle) {
		String position = title;
		
		if("팀장".equals(personalTitle)) {
			position = personalTitle;
		}
		
		return position;
	}
	
	private String setPositionCode(String position) {
		String positionCode = "";
		
		if ("4급사원".equals(position)) {
			positionCode = "L0";
		} else if ("5급사원".equals(position)) {
			positionCode = "L1";
		} else if ("PT".equals(position)) {
			positionCode = "L2";
		} else if ("대리".equals(position)) {
			positionCode = "L3";
		} else if ("차장".equals(position)) {
			positionCode = "L4";
		} else if ("과장".equals(position)) {
			positionCode = "L5";
		} else if ("부장".equals(position)) {
			positionCode = "L7";
		} else if ("팀장".equals(position)) {
			positionCode = "L8";
		} else if ("담당".equals(position)) {
			positionCode = "L10";
		}
		
		return positionCode;
	}
}
