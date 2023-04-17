 package com.iopts.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iopts.scheduler.drmJobs;
import com.skyun.app.util.config.AppConfig;

public class pServermain {

	private static String CONF_PATH = null;
	private static String LOGJ_PATH = null;
	private static String PID = null;
	public static String currentDir = null;
	
	private static String customer_id = "";
	public static void main(String[] args) {

		currentDir = System.getProperty("user.dir");
		File f = new File(currentDir);
		currentDir = f.getParent().toString();

		LOGJ_PATH = currentDir + "/conf/logbackDRMAction.xml";
		System.setProperty("logback.configurationFile", LOGJ_PATH);
		

		Logger logger = LoggerFactory.getLogger(pServermain.class);
		
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		AppConfig.setPID(getPID() + "");
		wrtiePID(AppConfig.getPID());

		customer_id = AppConfig.getProperty("config.customer");

		logger.info(">> Process ID :" + AppConfig.getPID());
		logger.info(">> Home Dir :" + AppConfig.currentDir);
		logger.info(">> System Version  2023-02-09");

		drmJobs drm = new drmJobs();

	}

	public static long getPID() {
		String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		return Long.parseLong(processName.split("@")[0]);
	}

	public static void wrtiePID(String pid) {
		BufferedWriter out = null;
		try {
			////////////////////////////////////////////////////////////////
			out = new BufferedWriter(new FileWriter(AppConfig.currentDir + "/pScheduleid"));

			out.write(pid);
			////////////////////////////////////////////////////////////////
		} catch (IOException e) {
			System.err.println(e); 
			System.exit(1);
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
