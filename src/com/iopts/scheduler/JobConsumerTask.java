package com.iopts.scheduler;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.ParseException;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.iopts.skyun.recon.vo.groupall.FindsCo;
import com.iopts.skyun.recon.vo.groupall.remediations;
import com.iopts.skyun.recon.vo.groupall.subpaths;
import com.iopts.skyun.recon.vo.groupall.summaryCo;
import com.skyun.app.util.config.AppConfig;
import com.skyun.app.util.config.IoptsCurl;
import com.skyun.recon.util.database.ibatis.SqlMapInstance;
import com.skyun.recon.util.database.ibatis.tr.DBInsertTable;
import com.skyun.recon.util.database.ibatis.vo.exceptionVo;
import com.skyun.recon.util.database.ibatis.vo.findlVo;
import com.skyun.recon.util.database.ibatis.vo.pathOwnerVo;
import com.skyun.recon.util.database.ibatis.vo.subpathVo;
import com.skyun.recon.util.database.ibatis.vo.summaryVo;
import com.skyun.recon.util.database.ibatis.vo.targetVo;

public class JobConsumerTask {
	private static Logger logger = LoggerFactory.getLogger(JobConsumerTask.class);

	private String gid = "";
	private String tid = "";
	private List<exceptionVo> exceptionList;

	private DBInsertTable tr = new DBInsertTable();
	private static SqlMapClient sqlMap = null;
	
	private String str_date = "";
	private String customer_id = "";
//	private static int ap_no = 0;

	public JobConsumerTask(targetVo info) {

		this.sqlMap = SqlMapInstance.getSqlMapInstance();

		this.gid = info.getGroup_id();
		this.tid = info.getTarget_id();

		this.customer_id = AppConfig.getProperty("config.customer");
		
//		String ap_count = (info.getAp_no() == null) ? "0" : info.getAp_no();
		int ap_no = info.getAp_no();
		
//		String user = AppConfig.getProperty("config.recon.user");
//		String pass = AppConfig.getProperty("config.recon.pawwsord");
		// String ip = AppConfig.getProperty("config.recon.ip");
		String user = (ap_no == 0) ? AppConfig.getProperty("config.recon.user") : AppConfig.getProperty("config.recon.user_"+(ap_no+1));
		String pass = (ap_no == 0) ? AppConfig.getProperty("config.recon.pawwsord") : AppConfig.getProperty("config.recon.pawwsord_"+(ap_no+1));
		String ip = (ap_no == 0) ? AppConfig.getProperty("config.recon.ip") : AppConfig.getProperty("config.recon.ip_"+(ap_no+1)) ;
		String port = AppConfig.getProperty("config.recon.port");
		String api_ver = AppConfig.getProperty("config.recon.api.version");
		
//		customer_id = AppConfig.getProperty("config.customer");

		str_date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		
		String curlurl = String.format("-k -X GET -u %s:%s https://%s:%s/%s/targets/%s/matchobjects?details=true&limit=10000000", user, pass, ip, port, api_ver, info.getTarget_id());
		logger.info("curlurl [" + curlurl + "]");
//		logger.info("targetVo [" + info.getPlatform() + "]");

		// HttpResponse response = curl(curlurl);

		String[] array = curlurl.split(" ");

		String jsonjunmun = "";

		String json_string;
		try {
			exceptionList = new ArrayList<>();
			
			if("MERITZ".equals(customer_id)) {
				exceptionList = tr.getSqlclient().openSession().queryForList("query.getExceptionListForOwner");
			}
			// json_string = EntityUtils.toString(response.getEntity());

			json_string = new IoptsCurl().opt(array).exec(null);
			if("3170593334469159466".equals(info.getTarget_id())) {
				logger.info(json_string);
			}
			if (json_string == null || json_string.length() < 1) {
				logger.error("Data Null Check IP or ID: " + curlurl);
			} else {

				JSONArray temp1 = new JSONArray(json_string);

				logger.info(info.getName() + " Find Files : " + temp1.length());

				for (int i = 0; i < temp1.length(); i++) {
					Gson gson = new Gson();
					jsonjunmun = temp1.get(i).toString();
					FindsCo f = gson.fromJson(jsonjunmun, FindsCo.class);

					findlVo fvo = new findlVo();

					
					if (f != null) {
						fvo.setGroup_id(gid);
						fvo.setTarget_id(tid);
						fvo.setAccount(f.getOwner());
						fvo.setOwner(AppConfig.account.get(tid + "_" + f.getOwner()));
						fvo.setPath(f.getPath());
						fvo.setFid(f.getId());
						fvo.setRegdate(str_date);
						
						if("HDcar".equals(customer_id)) {
							String owner = "";
							owner = (String) tr.getSqlclient().openSession().queryForObject("query.getOwnerFromAccountInfo", fvo);
							fvo.setOwner(owner);
						}
						
						// owner 추가
						if("MERITZ".equals(customer_id)) {
							for(exceptionVo vo : exceptionList) {
								String path = fvo.getPath();
								if(vo.getGroup_id().length() < path.length()) {
									if( tid.equals(vo.getTarget_id()) && 
											vo.getGroup_id().equals(path.substring(0, vo.getGroup_id().length()))
											){
										fvo.setOwner(vo.getOk_user_no());
									}
								}
							}
						}
						
						//logger.info("["+fvo.getTarget_id()+"] fid(" + fvo.getFid()+") :: " + fvo.getPath());
						//fvo.setHash_id((gid + tid + fvo.getPath()).hashCode() + "");
						fvo.setHash_id((tid + fvo.getPath()).hashCode() + "");
						fvo.setAp_no(ap_no);
						
						// 담당자 변경 내역 확인 후 추가
						if("MERITZ".equals(customer_id)) {
							String changed_owner = "";
							changed_owner = (String) tr.getSqlclient().openSession().queryForObject("query.getOwnerFromChargeUpdate", fvo);
							if(changed_owner != null && !"".equals(changed_owner)) {
								fvo.setOwner(changed_owner);
							}
						}
						
						if (AppConfig.account.get(tid + "_" + f.getOwner()) != null) {
							logger.debug("Find Path Owner :" + tid + f.getOwner() + "---> " + AppConfig.account.get(tid + "_" + f.getOwner()));
						}
						
						if("SH".equals(customer_id)) {
							if (fvo.getFid() != null && !fvo.getFid().equals("") && f.getSubs() == null) {		// fid가 있고, subpath가 없으면 통신 가능 여부 체크 
								if(doubleCheckFindData(fvo.getTarget_id(), fvo.getFid())){
									try {
										this.sqlMap.openSession().insert("insert.setFind", fvo);
									} catch (SQLException e) {
										String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
										fvo.setPath(f.getPath().replaceAll(match, "?"));
										logger.error("Error setFind SQLException  Target_ID:" + tid);
										logger.error("Error setFind SQLException  Path :" + fvo.getPath());
											
										DbInsertSetFind(fvo);
									}

									try {
										setSummary(fvo.getHash_id(), "T", f.getSummary(), ap_no);
									} catch (SQLException e) {
										logger.error("Error setSummary SQLException  Target_ID:" + tid);
										logger.error("Error setSummary SQLException " + e.getLocalizedMessage());
									}
								}
							}else if(f.getSubs() != null) {	// fid가 있고, subpath가 없으면 통신 가능 여부 체크
								try {
									if(RecursiveCall_sh(f.getSubs(), fvo.getHash_id(), ap_no) > 0) {
										try {
											this.sqlMap.openSession().insert("insert.setFind", fvo);
										} catch (SQLException e) {
											String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
											fvo.setPath(f.getPath().replaceAll(match, "?"));
											logger.error("Error setFind SQLException  Target_ID:" + tid);
											logger.error("Error setFind SQLException  Path :" + fvo.getPath());
												
											DbInsertSetFind(fvo);
										}

										try {
											setSummary(fvo.getHash_id(), "T", f.getSummary(), ap_no);
										} catch (SQLException e) {
											logger.error("Error setSummary SQLException  Target_ID:" + tid);
											logger.error("Error setSummary SQLException " + e.getLocalizedMessage());
										}
									}
								} catch (Exception e) {
									logger.error("Error RecursiveCall SQLException  Target_ID:" + tid);
									logger.error("Error RecursiveCall SQLException  " + e.getLocalizedMessage());
								}
							}
							
							/* shlee_20201028 */
						} else { // 신한아닌타사 
							
							try {
								this.sqlMap.openSession().insert("insert.setFind", fvo);
							} catch (SQLException e) {
								String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
								fvo.setPath(f.getPath().replaceAll(match, "?"));
								logger.error("Error setFind SQLException  Target_ID:" + tid);
								logger.error("Error setFind SQLException  Path :" + fvo.getPath());
								
								DbInsertSetFind(fvo);
							}
							
							try {
								setSummary(fvo.getHash_id(), "T", f.getSummary(), ap_no);
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								logger.error("Error setSummary SQLException  Target_ID:" + tid);
								logger.error("Error setSummary SQLException " + e.getLocalizedMessage());
								
							}
							
							
							
							if (f.getSubs() != null) {
								try {
									RecursiveCall(f.getSubs(), fvo.getHash_id(), ap_no);
									// KB
									//RecursiveCall(f.getSubs(), fvo.getHash_id(), fvo.getHash_id());
								} catch (SQLException e) {
									// TODO Auto-generated catch block
									logger.error("Error RecursiveCall SQLException  Target_ID:" + tid);
									logger.error("Error RecursiveCall SQLException  " + e.getLocalizedMessage());
								}
							}
						}
						
					}
				} // temp for
			} // else 
			
			// 삭제일 입력
			Map<String, Object> map = new HashMap<>();
			map.put("target_id", this.tid);
			map.put("regdate", str_date);
			map.put("ap_no", ap_no);
			
			tr.setDBInsertTable("update.setDeldate", map);
			
			logger.info(info.getName() + " ==> update deldate");
			
			// 검색시간 setting
			new SetDuration(info);
			
			// this.sqlMap.executeBatch();
			// this.sqlMap.commitTransaction();
			
			// insert DB server information
			if("Remote Access Only".equals(info.getPlatform())) {
				insertDBServerInfo(user, pass, ip, port, api_ver, info, ap_no);
			}
			
			if("HDcar".equals(customer_id)) {
				executePathOwner(info);
			}
			
		} catch (ParseException | SQLException e1) {
			logger.error("ParseException 1" + e1.getLocalizedMessage());
		} catch (Exception e1) {
			e1.printStackTrace();
		}  finally {
			/*
			 * try { this.sqlMap.endTransaction(); } catch (SQLException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 */
		}
	}


	private void executePathOwner(targetVo info) throws Exception {
		ArrayList<pathOwnerVo> list = new ArrayList<>();
		
		logger.info("["+info.getTarget_id()+"] "+info.getName()+" executePathOwner ");
		list = (ArrayList<pathOwnerVo>) this.sqlMap.openSession().queryForList("query.getPathOwner", info);
		
		for(int i=0; i<list.size(); i++) {
			pathOwnerVo vo = list.get(i);
			vo.setPath(vo.getPath().replaceAll("\\\\", "\\\\\\\\"));
			this.sqlMap.openSession().update("update.setPathOwner", list.get(i));
		}
		
	}

	private void DbInsertSetFind(findlVo f) {
		String path = f.getPath();
		
		try {
			this.sqlMap.openSession().insert("insert.setFind", f);
		} catch (SQLException e) {
			logger.error("Error setFind SQLException  Target_ID:" + tid);
			logger.error("Error setFind SQLException  Path :" + path);
			logger.error("Error setFind SQLException " + e.getLocalizedMessage());

		}
	}
	
	private String getRemediations(remediations remedi) {
		String remidation = remedi.getRemediation_status();
		
		return remidation;
	}
	
	private int RecursiveCall_sh(List<subpaths> subs, String pid, int ap_no) throws SQLException {
		int i = 0;
		int insert_cnt = 0;
		for (subpaths s : subs) {
			subpathVo fvo = new subpathVo();

			fvo.setGroup_id(gid);
			fvo.setTarget_id(tid);
			fvo.setParent_id(pid);
			fvo.setAccount(s.getOwner());
			fvo.setOwner(AppConfig.account.get(tid + "_" + s.getOwner()));

			String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
			fvo.setPath(s.getPath().replaceAll(match, " "));

			fvo.setInfo_id(s.getId());
			fvo.setNode_id((gid + tid + pid + fvo.getPath()).hashCode() + "");
			fvo.setAp_no(ap_no);
			// this.sqlMap.insert("insert.setSubpath", fvo);
			
			if (fvo.getInfo_id() != null && !fvo.getInfo_id().equals("") && s.getSubs() == null) {		// fid가 있고, subpath가 없으면 통신 가능 여부 체크 
				if(doubleCheckFindData(fvo.getTarget_id(), fvo.getInfo_id())){
					if(fvo.getParent_id().equals("-1592031543") || fvo.getParent_id().equals("-1462948824")) {
						logger.info("fvo.getInfo_id() :: " + fvo.getInfo_id()); 
					}
					
					i++;
					this.sqlMap.openSession().insert("insert.setSubpath", fvo);
					
					setSummary(fvo.getNode_id(), "S", s.getSummary(), ap_no);
					insert_cnt++;
				}
			} else if(s.getSubs() != null) {
				if(RecursiveCall_sh(s.getSubs(), fvo.getNode_id(), ap_no) > 0) {
					if(fvo.getParent_id().equals("-1592031543") || fvo.getParent_id().equals("-1462948824")) {
						logger.info("fvo.getInfo_id() :: " + fvo.getInfo_id()); 
					}
					
					i++;
					this.sqlMap.openSession().insert("insert.setSubpath", fvo);
					
					setSummary(fvo.getNode_id(), "S", s.getSummary(), ap_no);
					insert_cnt++;
				}
			}
		}
		
		return insert_cnt;
	}
	
	private void RecursiveCall(List<subpaths> subs, String pid, int ap_no) throws SQLException {
		int i = 0;
		for (subpaths s : subs) {
			subpathVo fvo = new subpathVo();

			fvo.setGroup_id(gid);
			fvo.setTarget_id(tid);
			fvo.setParent_id(pid);
			fvo.setAccount(s.getOwner());
			fvo.setOwner(AppConfig.account.get(tid + "_" + s.getOwner()));

			String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
			fvo.setPath(s.getPath().replaceAll(match, " "));

			fvo.setInfo_id(s.getId());
			fvo.setNode_id((gid + tid + pid + fvo.getPath()).hashCode() + "");
			fvo.setAp_no(ap_no);
			// this.sqlMap.insert("insert.setSubpath", fvo);
			
			
			if(fvo.getParent_id().equals("-1592031543") || fvo.getParent_id().equals("-1462948824")) {
				logger.info("fvo.getInfo_id() :: " + fvo.getInfo_id()); 
			}
				
				
			// KB 
			if(s.getRems() != null) {
				String remedition = getRemediations(s.getRems());
				fvo.setRemediation_status(remedition);
				if(i == 0)
					this.sqlMap.openSession().update("update.setFind", fvo);
			}
			i++;
			this.sqlMap.openSession().insert("insert.setSubpath", fvo);

			setSummary(fvo.getNode_id(), "S", s.getSummary(), ap_no);
			
			if (s.getSubs() != null) {
				RecursiveCall(s.getSubs(), fvo.getNode_id(), ap_no);
			}
		}
	}
	// KB Bank remediation
	/*private void RecursiveCall(List<subpaths> subs, String pid, String hashID) throws SQLException {
		int i = 0;
		for (subpaths s : subs) {
			subpathVo fvo = new subpathVo();
			
			fvo.setGroup_id(gid);
			fvo.setTarget_id(tid);
			fvo.setParent_id(pid);
			fvo.setAccount(s.getOwner());
			fvo.setOwner(AppConfig.account.get(tid + "_" + s.getOwner()));
			
			String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
			fvo.setPath(s.getPath().replaceAll(match, " "));
			
			fvo.setInfo_id(s.getId());
			fvo.setNode_id((gid + tid + pid + fvo.getPath()).hashCode() + "");
			// this.sqlMap.insert("insert.setSubpath", fvo);
			
			
			// KB 
			if(s.getRems() != null) {
				subpathVo fvo2 = new subpathVo();
				String remedition = getRemediations(s.getRems());
				fvo2.setParent_id(hashID);
				fvo2.setRemediation_status(remedition);
				if(i == 0)
					this.sqlMap.openSession().update("update.setFind", fvo2);
			}
			i++;
			this.sqlMap.openSession().insert("insert.setSubpath", fvo);
			
			setSummary(fvo.getNode_id(), "S", s.getSummary());
			
			if (s.getSubs() != null) {
				RecursiveCall(s.getSubs(), fvo.getNode_id(), hashID);
			}
		}
	}*/

	public boolean doubleCheckFindData(String tid, String info_id) {
		boolean result = true;
		
		String curlurl = String.format("-k -X GET -u %s:%s 'https://%s:%s/beta/targets/%s/matchobjects/%s?details=true'",
				AppConfig.getProperty("config.recon.user"), AppConfig.getProperty("config.recon.pawwsord"), AppConfig.getProperty("config.recon.ip"), AppConfig.getProperty("config.recon.port"),
				tid, info_id);

		//logger.info("doubleCheckFindData curlurl [" + curlurl + "]");

		String[] array = curlurl.split(" ");

		tr = new DBInsertTable();

		String json_string;
		json_string = new IoptsCurl().opt(array).exec(null);

		if (json_string == null || json_string.length() < 1) {
			
			logger.error("Data Null Check IP or ID: " + curlurl);

		} else {
			
			//logger.info("json_string :: [" + json_string+"]");
			//logger.info(json_string.contains("\"message\":\"Resource not found.\""));
			if(json_string.contains("\"message\":\"Resource not found.\"")){
				result = false;
			}
		}
		
		return result;
	}
	
	private void setSummary(String nid, String dtype, List<summaryCo> lst, int ap_no) throws SQLException {
		
		if (lst != null) {
			for (summaryCo c : lst) {
				summaryVo vo = new summaryVo(gid, tid, nid, dtype, c);
				vo.setAp_no(ap_no);
				this.sqlMap.openSession().insert("insert.setSummary", vo);
			}
		}
	}

	private void getServerInfo() {

	}


	private void insertDBServerInfo(String user, String pass, String ip, String port, String api_ver, targetVo info, int ap_no) {
//		logger.info("targetVo [" + info.getTarget_id() + "]");
//		logger.info("targetVo [" + info.getName() + "]");
//		logger.info("targetVo [" + info.getPlatform() + "]");
		
		String curlurl = String.format("-k -X GET -u %s:%s https://%s:%s/%s/targets/%s/isolated", user, pass, ip, port, api_ver, info.getTarget_id());
		logger.info("curlurl [" + curlurl + "]");
//		logger.info("targetVo [" + info.getPlatform() + "]");

		// HttpResponse response = curl(curlurl);

		String[] array = curlurl.split(" ");

		String jsonjunmun = "";

		String json_string;
		try {

			json_string = new IoptsCurl().opt(array).exec(null);

			if (json_string == null || json_string.length() < 1 || json_string.contains("Resource not found.")) {
				logger.error("Data Null Check IP or ID: " + curlurl);
			} else {

				JSONArray temp1 = new JSONArray(json_string);

				logger.info(info.getName() + " Find Files : " + temp1.length());
				
				for (int i = 0; i < temp1.length(); i++) {
					Gson gson = new Gson();
					jsonjunmun = temp1.get(i).toString();
					
					curlurl = String.format("-k -X GET -u %s:%s https://%s:%s/%s/targets/%s/rawisolated/%s", user, pass, ip, port, api_ver, info.getTarget_id(), jsonjunmun);
					logger.info("rawisolated curlurl [" + curlurl + "]");
					
					String[] raw_array = curlurl.split(" ");
					
					String raw_json;
					raw_json = new IoptsCurl().opt(raw_array).exec(null);
					
					JsonParser parser = new JsonParser();
					//logger.info("raw_json :: " + raw_json);
					if(raw_json.contains("This report has old format and is not supported")){
						logger.error("MalformedJsonException");
					} else {
						
						JsonObject raw_obj = (JsonObject) parser.parse(raw_json);
						
						String raw_label = raw_obj.get("label").toString();
						
						if(raw_label.contains("OneDrive")) {
							logger.info("One Drive Break");
							break;
						}
						
						int match_locations = Integer.parseInt(raw_obj.get("match_locations").toString().replaceAll("\"", ""));
						logger.info("match_locations :: " + match_locations);
						
						if(match_locations > 0) {
							JsonArray location_arr = raw_obj.getAsJsonArray("locations");
							
							for(int j=0; j<location_arr.size(); j++) {
								JsonObject loc_obj = (JsonObject) location_arr.get(j);
								
								String loc_path = "";
								String loc_catalog = "";
								String loc_table = "";
								String loc_schema = "";
								
								logger.info("loc_path :: " + loc_path);
								
								JsonArray meta_arr = (JsonArray) loc_obj.get("metadata");
								for(int meta_i=0; meta_i<meta_arr.size(); meta_i++) {
									JsonObject meta_obj = (JsonObject) meta_arr.get(meta_i);
									
									String label = meta_obj.get("label").toString().replaceAll("\"", "");
//									logger.info("label :: " + label);
									if("Catalog".equals(label)) {
										JsonArray val_arr = (JsonArray) meta_obj.get("values");
										loc_catalog = val_arr.get(0).toString().replaceAll("\"", "");
									}
									if("Table".equals(label)) {
										JsonArray val_arr = (JsonArray) meta_obj.get("values");
										loc_table = val_arr.get(0).toString().replaceAll("\"", "");
									}
									if("Schema".equals(label)) {
										JsonArray val_arr = (JsonArray) meta_obj.get("values");
										loc_schema = val_arr.get(0).toString().replaceAll("\"", "");
									}
									
//									logger.info("meta_obj :: " + meta_obj);
								}
								
								if(!"".equals(loc_catalog)) {
									loc_path = loc_catalog;
								}
								if(!"".equals(loc_schema)) {
									loc_path += "/" + loc_schema;
								}
								if(!"".equals(loc_table)) {
									loc_path += "/" + loc_table;
								}
								
//								logger.info("loc_path :: " + loc_path);
								
//								Map<String, Object> upt_data = new HashMap<>();
//								upt_data =  (Map<String, Object>) tr.getSqlclient().openSession().queryForObject("query.getHashId", map);
//								logger.info("upt_data :: " + upt_data.toString());
								
								JsonArray data_types = (JsonArray) loc_obj.get("data_types");
//								logger.info("data_types.soze() :: " + data_types.size());
//								logger.info("data_types :: " + data_types);
								
								List<String> columnList = new ArrayList<>();
								
								for(int dt_i=0; dt_i<data_types.size(); dt_i++) {
									JsonObject data_type = (JsonObject) data_types.get(dt_i);
									
									JsonArray data_meta_arr = (JsonArray) data_type.get("metadata");
									
									for(int ma_i=0; ma_i<data_meta_arr.size(); ma_i++) {
										JsonObject meta_obj = (JsonObject) data_meta_arr.get(ma_i);
										
										String label = meta_obj.get("label").toString().replaceAll("\"", "");
										if("Column".equals(label)) {
//											logger.info("values :: " + meta_obj.get("values").toString().replaceAll("\"", ""));
											JsonArray values = (JsonArray) meta_obj.get("values");
											
											for(int vi=0; vi<values.size(); vi++) {
												String value = values.get(vi).toString().replaceAll("\"", "");
												if(!columnList.contains(value)) {
//													logger.info("value :: " + value);
													columnList.add(value);
												}
											}
										}
									}
								}
								
//								logger.info("columnList :: " + columnList.toString());
								
								Collections.sort(columnList);
								
								String column = columnList.toString();
								column = column.substring(1, column.length()-1);
								
								Map<String, Object> map = new HashMap<>();
								map.put("target_id", info.getTarget_id());
								map.put("path", loc_path);
								map.put("column", column);
								map.put("ap_no", ap_no);
								
								logger.info("map :: " + map.toString());
								tr.setDBInsertTable("update.setColumn", map);
							}
							
						}
					}
					
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
