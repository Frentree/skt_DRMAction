 package com.iopts.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iopts.scheduler.ReconSchedulerJobs;
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

		LOGJ_PATH = currentDir + "/conf/logbackSchedule.xml";
		System.setProperty("logback.configurationFile", LOGJ_PATH);
		

		Logger logger = LoggerFactory.getLogger(pServermain.class);
		
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		AppConfig.setPID(getPID() + "");
		wrtiePID(AppConfig.getPID());

		customer_id = AppConfig.getProperty("config.customer");

		logger.info(">> Process ID :" + AppConfig.getPID());
		logger.info(">> Home Dir :" + AppConfig.currentDir);
		logger.info(">> System Version  2019-05-14__________________ ");
		logger.info(">> System Version  2019-08-08 (Mod)__________________ ");
		logger.info(">> System Version  2019-09-18 for Recon 2.0 ");
		logger.info(">> System Version  2019-09-18 for Owner Find,Subpath match account info");
		logger.info(">> System Version  2022-05-19 Patch list");
		logger.info(">> 	1) NH PIC Update");
		logger.info(">> 	2) Schedule All Update Formmat config.reconSchedule.startTime");
		logger.info(">> 	6> customer ID :: " + customer_id);

		new Thread(new ReconSchedulerJobs()).start();

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
